package com.fundroid.offstand.ui.lobby.findroom;


import android.util.Log;

import com.fundroid.offstand.data.DataManager;
import com.fundroid.offstand.data.model.Attendee;
import com.fundroid.offstand.data.model.ApiBody;
import com.fundroid.offstand.data.remote.ConnectionManager;
import com.fundroid.offstand.ui.base.BaseViewModel;
import com.fundroid.offstand.utils.rx.RxEventBus;
import com.fundroid.offstand.utils.rx.SchedulerProvider;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;

import static com.fundroid.offstand.core.AppConstant.RESULT_OK;
import static com.fundroid.offstand.core.AppConstant.ROOM_PORT;
import static com.fundroid.offstand.data.model.Attendee.EnumAvatar.JAN;
import static com.fundroid.offstand.data.remote.ApiDefine.API_ENTER_ROOM;
import static com.fundroid.offstand.data.remote.ApiDefine.API_ROOM_INFO;

public class FindRoomViewModel extends BaseViewModel<FindRoomNavigator> {

    private SchedulerProvider schedulerProvider;

    public FindRoomViewModel(DataManager dataManager, SchedulerProvider schedulerProvider) {
        super(dataManager, schedulerProvider);
        this.schedulerProvider = schedulerProvider;

        getCompositeDisposable().add(RxEventBus.getInstance().getEvents(String.class)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(result -> {
                    Log.d("lsc", "FindRoomViewModel result " + result);
                    switch (((ApiBody)result).getNo()) {
                        case API_ROOM_INFO:
                            getNavigator().goToRoomActivity();
                            break;
                    }
                }, onError -> {
                    Log.d("lsc","FindRoomViewModel onError " + onError);
                })
        );
    }

    private void enterRoom(InetAddress roomAddress, int roomPort) {
        Log.d("lsc", "FindRoomViewModel enterRoom " + roomAddress);
        getCompositeDisposable().add(ConnectionManager.createClientThread(roomAddress, roomPort)
                .andThen(Completable.timer(500, TimeUnit.MILLISECONDS))
                .andThen(ConnectionManager.sendMessage(new ApiBody(API_ENTER_ROOM, new Attendee("이승철", JAN.getIndex(), 10, 1))))
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(() -> {
                    Log.d("lsc", "FindRoomViewModel enterRoom result");
                }, onError -> {
                    Log.d("lsc", "FindRoomViewModel enterRoom onError " + onError);
                }));

    }

    public void onEnterRoomClick() {
        byte[] ipAddr = new byte[]{(byte) 192, (byte) 168, (byte) 40, (byte) 34};
        InetAddress addr = null;
        try {
            addr = InetAddress.getByAddress(ipAddr);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        enterRoom(addr, ROOM_PORT);
    }

    public void onNavBackClick() {
        getNavigator().goBack();
    }
}
