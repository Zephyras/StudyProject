package com.fundroid.offstand.data.remote;


import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.fundroid.offstand.data.model.ApiBody;
import com.fundroid.offstand.data.model.Card;
import com.fundroid.offstand.model.User;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

import static com.fundroid.offstand.core.AppConstant.RESULT_API_NOT_DEFINE;
import static com.fundroid.offstand.data.model.Card.setCardValue;
import static com.fundroid.offstand.data.remote.ApiDefine.API_BAN;
import static com.fundroid.offstand.data.remote.ApiDefine.API_BAN_BR;
import static com.fundroid.offstand.data.remote.ApiDefine.API_CARD_OPEN;
import static com.fundroid.offstand.data.remote.ApiDefine.API_DIE;
import static com.fundroid.offstand.data.remote.ApiDefine.API_DIE_BR;
import static com.fundroid.offstand.data.remote.ApiDefine.API_ENTER_ROOM;
import static com.fundroid.offstand.data.remote.ApiDefine.API_ENTER_ROOM_TO_OTHER;
import static com.fundroid.offstand.data.remote.ApiDefine.API_GAME_RESULT;
import static com.fundroid.offstand.data.remote.ApiDefine.API_GAME_RESULT_AVAILABLE;
import static com.fundroid.offstand.data.remote.ApiDefine.API_GAME_RESULT_BR;
import static com.fundroid.offstand.data.remote.ApiDefine.API_MOVE;
import static com.fundroid.offstand.data.remote.ApiDefine.API_MOVE_BR;
import static com.fundroid.offstand.data.remote.ApiDefine.API_OUT;
import static com.fundroid.offstand.data.remote.ApiDefine.API_OUT_BR;
import static com.fundroid.offstand.data.remote.ApiDefine.API_READY;
import static com.fundroid.offstand.data.remote.ApiDefine.API_READY_BR;
import static com.fundroid.offstand.data.remote.ApiDefine.API_READY_CANCEL;
import static com.fundroid.offstand.data.remote.ApiDefine.API_READY_CANCEL_BR;
import static com.fundroid.offstand.data.remote.ApiDefine.API_ROOM_INFO;
import static com.fundroid.offstand.data.remote.ApiDefine.API_SHUFFLE;
import static com.fundroid.offstand.data.remote.ApiDefine.API_SHUFFLE_AVAILABLE;
import static com.fundroid.offstand.data.remote.ApiDefine.API_SHUFFLE_BR;
import static com.fundroid.offstand.data.remote.ApiDefine.API_SHUFFLE_NOT_AVAILABLE;
import static com.fundroid.offstand.data.remote.ApiDefine.API_TEST;
import static com.fundroid.offstand.model.User.EnumStatus.CARDOPEN;
import static com.fundroid.offstand.model.User.EnumStatus.DIE;
import static com.fundroid.offstand.model.User.EnumStatus.INGAME;
import static com.fundroid.offstand.model.User.EnumStatus.READY;
import static com.fundroid.offstand.model.User.EnumStatus.STANDBY;

public class ConnectionManager {

    public enum EnumStatus {

        SHUFFLE_NOT_AVAILABLE(0), SHUFFLE_AVAILABLE(1), INGAME(2), GAME_RESULT_AVAILABLE(3), REGAME(4);

        private int enumStatus;

        EnumStatus(int enumStatus) {
            this.enumStatus = enumStatus;
        }

        public int getEnumStatus() {
            return enumStatus;
        }
    }

    private static ServerThread[] serverThreads;
    private static int serverCount;
    private static ClientThread clientThread;
    private static ServerSocket serverSocket;
    private static int roomMaxUser;
    private static EnumStatus roomStatus = EnumStatus.SHUFFLE_NOT_AVAILABLE;

    private static ArrayList<Integer> cards = new ArrayList<>();

    public static Completable createServerThread(int roomPort, int roomMaxUser) {
        ConnectionManager.roomMaxUser = roomMaxUser;
        return Completable.create(subscriber -> {
            serverSocket = new ServerSocket(roomPort);
            serverThreads = new ServerThread[roomMaxUser];
            subscriber.onComplete();   // accept에서 blocking 되니 방장 클라이언트가 붙기전에 보냄
            socketAcceptLoop();
        });
    }

    private static void socketAcceptLoop() {
        while (serverCount != roomMaxUser) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                Log.e("lsc", "socketAcceptLoop e " + e.getMessage());
            }
            ServerThread serverThread = new ServerThread(socket);
            serverThreads[serverCount] = serverThread;
            serverCount++;
            new Thread(serverThread).start();
        }
    }

    private static ArrayList<User> swapToFirst(ArrayList<User> users, int seatNo) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getSeat() == seatNo) {
                Collections.swap(users, 0, i);
            }
        }
        return users;
    }

    public static Observable<ApiBody> serverProcessor(String apiBodyStr) {
        ApiBody apiBody = new Gson().fromJson(apiBodyStr, ApiBody.class);
        switch (apiBody.getNo()) {
            case API_ENTER_ROOM:
                return
                        setUserSeatNo(apiBody)
                                .flatMap(seatNo -> Observable.zip(sendMessage(new ApiBody(API_ROOM_INFO, swapToFirst((ArrayList<User>) Stream.of(serverThreads).withoutNulls().map(serverThread -> serverThread.getUser()).collect(Collectors.toList()), seatNo)), seatNo),
                                        broadcastMessageExceptOne(new ApiBody(API_ENTER_ROOM_TO_OTHER, apiBody.getUser()), seatNo), (firstOne, secondOne) -> firstOne))
                                .concatMap(firstOne -> setRoomStatus())
                                .concatMap(ConnectionManager::sendToHost);

            case API_READY:
                return setUserStatus(apiBody.getNo(), apiBody.getSeatNo())
                        .andThen(setRoomStatus())
                        .concatMap(ConnectionManager::sendToHost)
                        .concatMap(result -> broadcastMessage(new ApiBody(API_READY_BR, apiBody.getSeatNo())));

            case API_READY_CANCEL:
                return setUserStatus(apiBody.getNo(), apiBody.getSeatNo())
                        .andThen(setRoomStatus())
                        .concatMap(ConnectionManager::sendToHost)
                        .concatMap(result -> broadcastMessage(new ApiBody(API_READY_CANCEL_BR, apiBody.getSeatNo())));

            case API_BAN:
                return broadcastMessageExceptOne(new ApiBody(API_BAN_BR, apiBody.getSeatNo()), apiBody.getSeatNo())
                        .concatMap(result -> closeServerSocket(apiBody.getSeatNo()))
                        .concatMap(result -> setRoomStatus())
                        .concatMap(ConnectionManager::sendToHost);

            case API_MOVE:
                return setUserSeatNo(apiBody.getSeatNo(), apiBody.getSeatNo2())
                        .andThen(broadcastMessage(new ApiBody(API_MOVE_BR, apiBody.getSeatNo(), apiBody.getSeatNo2())));

            case API_SHUFFLE:
                return
                        shuffle((ArrayList<ServerThread>) Stream.of(serverThreads).withoutNulls().collect(Collectors.toList()))
                                .filter(pair -> {
                                    Log.d("lsc", "shuffle roomStatus 1 " + roomStatus);
                                    Log.d("lsc", "shuffle roomStatus 2 " + pair.second.getStatus());
                                    Log.d("lsc", "shuffle roomStatus 3 " + (pair.second.getStatus() == CARDOPEN.getEnumStatus()));
                                    return roomStatus == EnumStatus.REGAME ? (pair.second.getStatus() == CARDOPEN.getEnumStatus()) : true;
                                })  //Todo : REGAME일 경우 CARDOPEN 필터링
                                .flatMap(pair -> sendMessage(new ApiBody(API_SHUFFLE_BR, pair.second.getCards().first, pair.second.getCards().second), pair.first));

            case API_DIE:
                return setUserStatus(apiBody.getNo(), apiBody.getSeatNo())
                        .andThen(setRoomStatus())
                        .concatMap(ConnectionManager::sendToHost)
                        .concatMap(result -> sendMessage(new ApiBody(API_DIE_BR, apiBody.getSeatNo()), apiBody.getSeatNo()));

            case API_CARD_OPEN:
                return setUserStatus(apiBody.getNo(), apiBody.getSeatNo())
                        .andThen(setRoomStatus())
                        .concatMap(ConnectionManager::sendToHost)
                        .concatMap(result -> Observable.just(new ApiBody(RESULT_API_NOT_DEFINE)));

            case API_GAME_RESULT:
                return setCardSumAndLevel((ArrayList<User>) Stream.of(serverThreads).withoutNulls().map(serverThread -> serverThread.getUser()).collect(Collectors.toList()))
                        .flatMap(ConnectionManager::setSumRebalance)
                        .flatMap(users -> sortByUserSum())
                        .flatMap(ConnectionManager::checkRematch)
                        .flatMapObservable(users -> broadcastMessage(new ApiBody(API_GAME_RESULT_BR, users)));


            case API_OUT:
                //Todo : 배열에 다 찰 경우 다시 loop 돌리는 로직 추가해야됨
                if (apiBody.getSeatNo().equals(serverThreads[0].getUser().getSeat())) {
                    return closeAllServerSocket();
                } else {
                    return broadcastMessageExceptOne(new ApiBody(API_OUT_BR, apiBody.getSeatNo()), apiBody.getSeatNo())
                            .concatMap(result -> closeServerSocket(apiBody.getSeatNo()))
                            .concatMap(result -> setRoomStatus())
                            .concatMap(ConnectionManager::sendToHost);
                }


            case API_TEST:
                for (User user : Stream.of(serverThreads).withoutNulls().map(serverThread -> serverThread.getUser()).collect(Collectors.toList())) {
                    Log.d("lsc", "user " + user);
                }
                return Observable.just(new ApiBody(RESULT_API_NOT_DEFINE));

            default:
                Log.d("lsc", "ConnectionManager default api " + apiBody);
                return Observable.just(new ApiBody(RESULT_API_NOT_DEFINE));
        }
    }

    private static Observable<Integer> setUserSeatNo(ApiBody apiBody) {
        return Observable.create(subscriber -> {
            int seatNo = 1;
            for (ServerThread serverThread : Stream.of(serverThreads).withoutNulls().collect(Collectors.toList())) {
                if (serverThread.getUser() == null) {
                    apiBody.getUser().setSeat(seatNo);
                    serverThread.setUser(apiBody.getUser());
                    subscriber.onNext(seatNo);
                } else {
                    if (serverThread.getUser().getSeat() == seatNo) {
                        seatNo += 1;
                    }
                }
            }
        });
    }

    private static Observable<ApiBody> closeServerSocket(int seatNo) {
        return Observable.create(subscriber -> {
            for (int index = 0; index < serverThreads.length; index++) {
                if (serverThreads[index] != null && serverThreads[index].getUser() != null) {
                    if (serverThreads[index].getUser().getSeat().equals(seatNo)) {
                        Log.d("lsc", "closeServerSocket seatNo " + seatNo);
                        serverThreads[index].getSocket().close();
                        serverThreads[index] = null;
                        serverCount--;
                    }
                }
            }
            subscriber.onNext(new ApiBody(RESULT_API_NOT_DEFINE));
        });
    }

    private static Observable<ApiBody> closeAllServerSocket() {
        return Observable.create(subscriber -> {
            for (int index = 0; index < serverThreads.length; index++) {
                if (serverThreads[index] != null && serverThreads[index].getUser() != null) {
                    serverThreads[index].getSocket().close();
                    serverThreads[index] = null;
                    serverCount--;
                }
            }
            serverSocket.close();
            serverSocket = null;
        });
    }

    public static Completable createClientThread(@Nullable InetAddress serverIp, int serverPort) {
        return Completable.create(subscriber -> {
            Socket socket = new Socket();
            clientThread = new ClientThread(socket, (serverIp == null) ? InetAddress.getLocalHost() : serverIp, serverPort);
            new Thread(clientThread).start();
            subscriber.onComplete();
        });
    }

    private static Completable setUserSeatNo(int selectedSeat, int targetSeat) {
        boolean isTargetSeatEmpty = Stream.of(serverThreads).withoutNulls().filter(serverThread -> serverThread.getUser().getSeat().equals(targetSeat)).count() == 0;
        if (isTargetSeatEmpty) {
            for (ServerThread serverThread : Stream.of(serverThreads).withoutNulls().collect(Collectors.toList())) {
                if (serverThread.getUser().getSeat().equals(selectedSeat)) {
                    serverThread.getUser().setSeat(targetSeat);
                }
            }
        } else {
            for (ServerThread serverThread : Stream.of(serverThreads).withoutNulls().collect(Collectors.toList())) {
                if (serverThread.getUser().getSeat().equals(selectedSeat)) {
                    serverThread.getUser().setSeat(-1);
                }
            }
            for (ServerThread serverThread : Stream.of(serverThreads).withoutNulls().collect(Collectors.toList())) {
                if (serverThread.getUser().getSeat().equals(targetSeat)) {
                    serverThread.getUser().setSeat(selectedSeat);
                }
            }
            for (ServerThread serverThread : Stream.of(serverThreads).withoutNulls().collect(Collectors.toList())) {
                if (serverThread.getUser().getSeat().equals(-1)) {
                    serverThread.getUser().setSeat(targetSeat);
                }
            }
        }
        return Completable.complete();
    }

    private static Completable setUserStatus(int apiNo, int seatNo) {
        for (ServerThread serverThread : Stream.of(serverThreads).withoutNulls().collect(Collectors.toList())) {
            if (serverThread.getUser().getSeat().equals(seatNo)) {
                switch (apiNo) {
                    case API_READY:
                        serverThread.getUser().setStatus(READY.getEnumStatus());
                        break;

                    case API_READY_CANCEL:
                        serverThread.getUser().setStatus(STANDBY.getEnumStatus());
                        break;

                    case API_CARD_OPEN:
                        serverThread.getUser().setStatus(CARDOPEN.getEnumStatus());
                        break;

                    case API_DIE:
                        serverThread.getUser().setStatus(DIE.getEnumStatus());
                        break;
                }
            }
        }

        return Completable.complete();
    }

    private static Observable<EnumStatus> setRoomStatus() {
        return Observable.create(subscriber -> {
            // 방장 제외한 나머지 User 리스트
            // 모두 ready 면 방장에게 셔플 가능 api 전송
            int userCountExceptHost = (int) Stream.of(serverThreads)
                    .withoutNulls()
                    .filterNot(serverThread -> serverThread.getUser().isHost())
                    .count();

            int readyUserCount = (int) Stream.of(serverThreads)
                    .withoutNulls()
                    .filterNot(serverThread -> serverThread.getUser().isHost())
                    .filter(serverThread -> serverThread.getUser().getStatus() == READY.getEnumStatus())
                    .count();

            int inGameUserCount = (int) Stream.of(serverThreads) // 게임 시작 후 카드 오픈 or DIE or 나가기를 하지 않은 User 카운트
                    .withoutNulls()
                    .filter(serverThread -> serverThread.getUser().getStatus() == INGAME.getEnumStatus())
                    .count();

            switch (roomStatus) {
                case SHUFFLE_NOT_AVAILABLE:
                case SHUFFLE_AVAILABLE:
                    if (userCountExceptHost == readyUserCount) {
                        roomStatus = EnumStatus.SHUFFLE_AVAILABLE;
                    } else {
                        roomStatus = EnumStatus.SHUFFLE_NOT_AVAILABLE;
                    }
                    break;

                case INGAME:
                    if (inGameUserCount == 0) {
                        roomStatus = EnumStatus.GAME_RESULT_AVAILABLE;
                    } else {
                        roomStatus = EnumStatus.INGAME;
                    }
                    break;

                case REGAME:
                    roomStatus = EnumStatus.INGAME;
                    break;
            }
            subscriber.onNext(roomStatus);
        });
    }

    private static Observable<ApiBody> sendToHost(EnumStatus roomStatus) {
        switch (roomStatus) {
            case SHUFFLE_AVAILABLE:
                return sendMessage(new ApiBody(API_SHUFFLE_AVAILABLE), serverThreads[0]);

            case SHUFFLE_NOT_AVAILABLE:
                return sendMessage(new ApiBody(API_SHUFFLE_NOT_AVAILABLE), serverThreads[0]);

            case GAME_RESULT_AVAILABLE:
                return sendMessage(new ApiBody(API_GAME_RESULT_AVAILABLE), serverThreads[0]);

            default:
                return Observable.just(new ApiBody(RESULT_API_NOT_DEFINE));
        }
    }

    public static Single<ArrayList<User>> setCardSumAndLevel(ArrayList<User> users) {
        return Single.create(subscriber -> {
            for (User user : Stream.of(users).filter(user -> user.getStatus() == CARDOPEN.getEnumStatus() || user.getStatus() == DIE.getEnumStatus()).toList()) {
                setCardValue(user);
                Log.d("lsc", "setCardSumAndLevel " + user);
            }
            subscriber.onSuccess(users);
        });
    }

    private static Single<ArrayList<User>> sortByUserSum() {
        return Single.create(subscriber -> {
            ArrayList<User> targetUsers = (ArrayList<User>) Stream.of(serverThreads).withoutNulls().map(serverThread -> serverThread.getUser()).collect(Collectors.toList());
            if (targetUsers.size() > 1) {
                Collections.sort(targetUsers);
                Collections.reverse(targetUsers);
            }
            subscriber.onSuccess(targetUsers);
        });
    }

    private static Single<ArrayList<User>> checkRematch(ArrayList<User> users) {
        return Single.create(subscriber -> {
            //승리자 LEVEL이 3 또는 7일 경우
            if (users.get(0).getCardLevel() == Card.EnumCardLevel.LEVEL3.getCardLevel() || users.get(0).getCardLevel() == Card.EnumCardLevel.LEVEL7.getCardLevel()) {
                roomStatus = EnumStatus.REGAME;
            }

            //카드 급이 같을 경우 (죽지 않고 동점이 아닌 사람의 STATUS 를 INGAME으로 셋
            Stream.of(serverThreads).withoutNulls().map(serverThread -> serverThread.getUser())
                    .filterNot(user -> user.getStatus() == DIE.getEnumStatus())
                    .filterNot(user -> users.get(0).getCardSum() == user.getCardSum())
                    .map(loseUser -> {
                        loseUser.setStatus(INGAME.getEnumStatus());
                        return loseUser;
                    }).collect(Collectors.toList());

            if (users.size() > 1 && users.get(0).getCardSum() == users.get(1).getCardSum()) {
                roomStatus = EnumStatus.REGAME;
            }
            subscriber.onSuccess(users);
        });
    }

    private static Single<ArrayList<User>> setSumRebalance(ArrayList<User> users) {
        return Single.create(subscriber -> {
            Log.d("lsc", "setSumRebalance start");
            if (Stream.of(users).filter(user -> user.getCardLevel() == Card.EnumCardLevel.LEVEL4.getCardLevel()).count() == 0) {
                Log.d("lsc", "setSumRebalance 땡 없음");
                Stream.of(users).filter(user -> user.getCardLevel() == Card.EnumCardLevel.LEVEL5.getCardLevel()).findFirst().ifPresent(level9User -> level9User.setCardSum(0));
            }
            if (Stream.of(users).filter(user -> user.getCardLevel() == Card.EnumCardLevel.LEVEL8.getCardLevel()).count() == 0) {
                Log.d("lsc", "setSumRebalance 광땡 없음");
                Stream.of(users).filter(user -> user.getCardLevel() == Card.EnumCardLevel.LEVEL9.getCardLevel()).findFirst().ifPresent(level9User -> level9User.setCardSum(0));
            }

            Log.d("lsc", "setSumRebalance end");
            subscriber.onSuccess(users);
        });
    }

    private static Observable<Pair<ServerThread, User>> shuffle(ArrayList<ServerThread> serverThreads) {
        cards.clear();
        for (int i = 1; i < 21; i++) {
            cards.add(i);
        }
        Collections.shuffle(cards);
        return Observable.create(subscriber -> {
            for (int i = 0; i < serverThreads.size(); i++) {
                Log.d("lsc", "shuffle " + cards.get(i * 2) + ", " + cards.get((i * 2) + 1));
                if (cards.get(i * 2) < cards.get((i * 2) + 1)) {
                    serverThreads.get(i).getUser().setCards(new Pair<>(cards.get(i * 2), cards.get((i * 2) + 1)));
                } else {
                    serverThreads.get(i).getUser().setCards(new Pair<>(cards.get((i * 2) + 1), cards.get(i * 2)));
                }
                if (roomStatus != EnumStatus.REGAME)
                    serverThreads.get(i).getUser().setStatus(INGAME.getEnumStatus());

                //card test
                // 2P, 3P 동점
//                serverThreads.get(0).getUser().setCards(new Pair<>(2, 8));
//                serverThreads.get(1).getUser().setCards(new Pair<>(4, 5));
//                serverThreads.get(2).getUser().setCards(new Pair<>(14, 15));
                // 3P 구사
//                serverThreads.get(0).getUser().setCards(new Pair<>(2, 8));
//                serverThreads.get(1).getUser().setCards(new Pair<>(4, 5));
//                serverThreads.get(2).getUser().setCards(new Pair<>(14, 19));
//                // 1P 8땡 2P 땡잡이 3P 구사
//                serverThreads.get(0).getUser().setCards(new Pair<>(8, 18));
//                serverThreads.get(1).getUser().setCards(new Pair<>(3, 7));
//                serverThreads.get(2).getUser().setCards(new Pair<>(14, 19));
//                // 1P 8땡 2P 땡잡이 3P 멍구사
//                serverThreads.get(0).getUser().setCards(new Pair<>(8, 18));
//                serverThreads.get(1).getUser().setCards(new Pair<>(3, 7));
//                serverThreads.get(2).getUser().setCards(new Pair<>(4, 9));

                //card test end
                subscriber.onNext(new Pair<>(serverThreads.get(i), serverThreads.get(i).getUser()));
            }
            roomStatus = EnumStatus.INGAME;
            subscriber.onComplete();
        });
    }

    private static Observable<ApiBody> broadcastMessage(ApiBody message) {
        Log.d("lsc", "ConnectionManager broadcastMessage " + message);
        return Observable.create(subscriber -> {

            for (ServerThread serverThread : serverThreads) {
                if (serverThread != null)
                    serverThread.getStreamToClient().writeUTF(message.toString());
            }
            subscriber.onNext(message);
        });
    }

    private static Observable<ApiBody> broadcastMessageExceptOne(ApiBody message, int seatNo) {
        return Observable.create(subscriber -> {

            for (ServerThread serverThread : Stream.of(serverThreads).withoutNulls().filterNot(serverThread -> serverThread.getUser().getSeat() == seatNo).collect(Collectors.toList())) {
                serverThread.getStreamToClient().writeUTF(message.toString());
            }
            subscriber.onNext(message);
        });
    }

    private static Observable<ApiBody> sendMessage(ApiBody message, int seatNo) {
        return Observable.create(subscriber -> {

            Stream.of(serverThreads)
                    .withoutNulls()
                    .filter(serverThread -> serverThread.getUser().getSeat() == seatNo)
                    .findFirst()
                    .get()
                    .getStreamToClient()
                    .writeUTF(message.toString());

            subscriber.onNext(message);
        });
    }

    private static Observable<ApiBody> sendMessage(ApiBody message, ServerThread serverThread) {
        return Observable.create(subscriber -> {
            serverThread.getStreamToClient().writeUTF(message.toString());
            subscriber.onNext(message);
        });
    }

    public static Completable sendMessage(ApiBody message) {
        return Completable.create(subscriber -> {
            clientThread.getStreamToServer().writeUTF(message.toString());
            subscriber.onComplete();
        });
    }
}
