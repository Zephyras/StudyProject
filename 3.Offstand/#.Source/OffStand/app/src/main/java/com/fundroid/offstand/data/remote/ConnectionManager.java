package com.fundroid.offstand.data.remote;

import android.util.Log;

import androidx.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.fundroid.offstand.data.model.ApiBody;
import com.fundroid.offstand.data.model.Attendee;
import com.google.gson.Gson;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

import static com.fundroid.offstand.core.AppConstant.RESULT_API_NOT_DEFINE;
import static com.fundroid.offstand.core.AppConstant.RESULT_OK;
import static com.fundroid.offstand.data.remote.ApiDefine.API_CARD_OPEN;
import static com.fundroid.offstand.data.remote.ApiDefine.API_DIE;
import static com.fundroid.offstand.data.remote.ApiDefine.API_DIE_BR;
import static com.fundroid.offstand.data.remote.ApiDefine.API_ENTER_ROOM;
import static com.fundroid.offstand.data.remote.ApiDefine.API_ENTER_ROOM_TO_OTHER;
import static com.fundroid.offstand.data.remote.ApiDefine.API_OUT;
import static com.fundroid.offstand.data.remote.ApiDefine.API_OUT_BR;
import static com.fundroid.offstand.data.remote.ApiDefine.API_READY;
import static com.fundroid.offstand.data.remote.ApiDefine.API_READY_BR;
import static com.fundroid.offstand.data.remote.ApiDefine.API_READY_CANCEL;
import static com.fundroid.offstand.data.remote.ApiDefine.API_READY_CANCEL_BR;
import static com.fundroid.offstand.data.remote.ApiDefine.API_ROOM_INFO;

// 한번에 roomMaxAttendee만큼

public class ConnectionManager {

    private static ServerThread[] serverThreads;
    private static int serverCount;
    private static ClientThread clientThread;
    private static ServerSocket serverSocket;

    public static Completable createServerThread(int roomPort, int roomMaxUser) {
        return Completable.create(subscriber -> {
            serverSocket = new ServerSocket(roomPort);
            serverThreads = new ServerThread[roomMaxUser];
            while (serverCount != roomMaxUser) {
                if ((Stream.of(serverThreads).filter(thread -> thread == null).count()) == roomMaxUser) {
                    subscriber.onComplete();   // accept에서 blocking 되니 방장 클라이언트가 붙기전에 보냄
                }
                Socket socket = serverSocket.accept();
                ServerThread serverThread = new ServerThread(socket);
                serverThreads[serverCount] = serverThread;
                serverCount++;
                new Thread(serverThread).start();
            }
        });
    }

    public static Observable<Integer> serverProcessor(String apiBodyStr) {
        ApiBody apiBody = new Gson().fromJson(apiBodyStr, ApiBody.class);
        Log.d("lsc", "serverProcessor apiBody " + apiBody);

        switch (apiBody.getNo()) {
            case API_ENTER_ROOM:
                int newUserServerIndex = -1;
                for (int index = 0; index < serverThreads.length; index++) {
                    if (serverThreads[index] != null && serverThreads[index].getAttendee() == null) {
                        newUserServerIndex = index;
                        apiBody.getAttendee().setSeatNo(index + 1);
                        serverThreads[index].setAttendee(apiBody.getAttendee());
                    }
                }

                return Observable.zip(
                        sendMessage(new ApiBody(API_ROOM_INFO, (ArrayList<Attendee>) Stream.of(serverThreads).filter(serverThread -> serverThread != null).map(serverThread -> serverThread.getAttendee()).collect(Collectors.toList())), newUserServerIndex),
                        broadcastMessageExceptOne(new ApiBody(API_ENTER_ROOM_TO_OTHER, apiBody.getAttendee()), newUserServerIndex),
                        (firstOne, secondOne) -> RESULT_OK
                );

            case API_READY:
                // 모든 유저 레디 시 방장 게임 시작 버튼 활성화
                return broadcastMessage(new ApiBody(API_READY_BR, apiBody.getSeatNo()));

            case API_DIE:
                // 죽지 않은 User가 1명 밖에 안남을 경우 게임 결과 이동
                return broadcastMessage(new ApiBody(API_DIE_BR, apiBody.getSeatNo()));

            case API_CARD_OPEN:
                // 모든 유저 카드 오픈 시 방장 게임 시작 버튼 활성화
                return Observable.just(RESULT_OK);

            case API_OUT:
                return broadcastMessage(new ApiBody(API_OUT_BR, apiBody.getSeatNo()));

            case API_READY_CANCEL:
                return broadcastMessage(new ApiBody(API_READY_CANCEL_BR, apiBody.getSeatNo()));

            default:
                return Observable.just(RESULT_API_NOT_DEFINE);
        }
    }

    public static Completable createClientThread(@Nullable InetAddress serverIp, int serverPort) {
        return Completable.create(subscriber -> {
            Socket socket = new Socket();
            clientThread = new ClientThread(socket, (serverIp == null) ? InetAddress.getLocalHost() : serverIp, serverPort);
            new Thread(clientThread).start();
//            subscriber.onNext(RESULT_OK);
            subscriber.onComplete();
        });
    }

    public static Observable test() {
        return Observable.create(subscriber -> {
            Log.d("lsc", "test complete");
            subscriber.onNext(1);
            subscriber.onComplete();
        });
    }

    public static Observable test2() {
        return Observable.create(subscriber -> {
            Log.d("lsc", "test2 complete");
            subscriber.onComplete();
        });
    }

    public static Observable broadcastMessage(ApiBody message) {
        return Observable.create(subscriber -> {
            for (int index = 0; index < serverThreads.length; index++) {
                if (serverThreads[index] != null)
                    serverThreads[index].getStreamToClient().writeUTF(message.toString());
            }
            subscriber.onNext(RESULT_OK);
        });
    }

    private static Observable<Integer> broadcastMessageExceptOne(ApiBody message, int serverIndex) {
        return Observable.create(subscriber -> {
            for (int index = 0; index < serverThreads.length; index++) {
                if (serverThreads[index] != null && index != serverIndex)
                    serverThreads[index].getStreamToClient().writeUTF(message.toString());
            }
            subscriber.onNext(RESULT_OK);
        });
    }

    private static Observable<Integer> sendMessage(ApiBody message, int serverIndex) {
        return Observable.create(subscriber -> {
            serverThreads[serverIndex].getStreamToClient().writeUTF(message.toString());
            subscriber.onNext(RESULT_OK);
        });
    }

    public static Completable sendMessage(ApiBody message) {
        return Completable.create(subscriber -> {
            clientThread.getStreamToServer().writeUTF(message.toString());
            subscriber.onComplete();
        });
    }
}
