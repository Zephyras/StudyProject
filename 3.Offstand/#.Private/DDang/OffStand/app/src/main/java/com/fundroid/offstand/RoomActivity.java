package com.fundroid.offstand;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fundroid.offstand.model.User;
import com.fundroid.offstand.model.UserWrapper;

import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RoomActivity extends AppCompatActivity implements View.OnTouchListener, View.OnDragListener {
//public class RoomActivity extends AppCompatActivity {

    static final String TAG = "[ROOM]";

    // org
    @BindView(R.id.btn_table)
    Button btn_table;
    @BindView(R.id.btn_ban)
    Button btn_ban;
    @BindView(R.id.btn_shuffle)
    Button btn_shuffle;
    @BindView(R.id.btn_ready)
    Button btn_ready;
    @BindView(R.id.btn_1)
    Button btn_1;
    @BindView(R.id.btn_2)
    Button btn_2;
    @BindView(R.id.btn_3)
    Button btn_3;
    @BindView(R.id.btn_4)
    Button btn_4;

    // real
    @BindView(R.id.room_image_start)
    ImageView image_start;
    @BindView(R.id.room_image_exit)
    ImageView image_exit;
    @BindView(R.id.room_image_ban)
    ImageView image_ban;

    @BindView(R.id.room_avatar_1)
    ImageView image_avatar_1;
    @BindView(R.id.room_avatar_2)
    ImageView image_avatar_2;
    @BindView(R.id.room_avatar_3)
    ImageView image_avatar_3;
    @BindView(R.id.room_avatar_4)
    ImageView image_avatar_4;

    @BindView(R.id.room_name_1)
    TextView text_user_1;
    @BindView(R.id.room_name_2)
    TextView text_user_2;
    @BindView(R.id.room_name_3)
    TextView text_user_3;
    @BindView(R.id.room_name_4)
    TextView text_user_4;

    int nUserCount = 0;
    int nReadyCount = 0;

    ImageView selectedAvatar = null;

    // 방장 여부
    boolean bHost;

    User currentUser;
    User[] allUser;
    // end of real


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room4);

        // 버터나이프 사용
        ButterKnife.bind(this);

        btn_shuffle.setOnTouchListener(this);
        btn_ban.setOnTouchListener(this);
        btn_ready.setOnTouchListener(this);

        btn_shuffle.setOnDragListener(this);
        btn_ban.setOnDragListener(this);
        btn_ready.setOnDragListener(this);

        btn_1.setOnTouchListener(this);
        btn_2.setOnTouchListener(this);
        btn_3.setOnTouchListener(this);
        btn_4.setOnTouchListener(this);

        btn_1.setOnDragListener(this);
        btn_2.setOnDragListener(this);
        btn_3.setOnDragListener(this);
        btn_4.setOnDragListener(this);

        initListener();

        // TODO: 방장인지 체크
        // 방만들기로 들어오는 경우 true, 아니면 false
        bHost = true;

        initRoom();
        initUser();

        for (int i = 1; i < nUserCount+1; i++) {
            drawUser(allUser[i]);
        }
    }

    @OnClick({R.id.room_image_start,
              R.id.room_image_ban,
              R.id.room_image_exit,
//              R.id.room_image_ready
    })
    public void clicked(ImageView view) {
        Log.d(TAG, "clicked");
        view.setSelected(true);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        View.DragShadowBuilder mShadow = new View.DragShadowBuilder(v);
        ClipData.Item item = new ClipData.Item(v.getTag().toString());
        String[] mimeTypes = {ClipDescription.MIMETYPE_TEXT_PLAIN};
        ClipData data = new ClipData(v.getTag().toString(), mimeTypes, item);

        int eventPadTouch = event.getAction();

        switch (eventPadTouch) {
            case MotionEvent.ACTION_DOWN:
                switch (v.getId()) {
                    case R.id.btn_shuffle:
                        Toast.makeText(getApplicationContext(), "셔플", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.btn_ready:
                        Toast.makeText(getApplicationContext(), "준비", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.btn_ban:
                        Toast.makeText(getApplicationContext(), "내보내기", Toast.LENGTH_SHORT).show();
                        break;

                    case R.id.room_image_start:
//                        image_start.setPressed(true);
                        break;
                    case R.id.room_image_ban:
//                        image_ban.setPressed(true);
                        break;
                    case R.id.room_image_exit:
//                        image_exit.setPressed(true);
                        break;


                    case R.id.btn_1:
                    case R.id.btn_2:
                    case R.id.btn_3:
                    case R.id.btn_4:

                    case R.id.room_avatar_1:
                    case R.id.room_avatar_2:
                    case R.id.room_avatar_3:
                    case R.id.room_avatar_4:

                        if (!bHost) {
//                        if (!isHost((ImageView)v)) {
                            Log.d(TAG, "방장 아님");
                            Toast.makeText(getApplicationContext(), "방장만 강퇴시킬수 있습니다.", Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        if (!isExist((ImageView)v)) {
                            Log.d(TAG, "빈 자리");
                            return false;
                        }
                        Log.d(TAG, "Drag 시작");
                        selectedAvatar = (ImageView)v;

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            v.startDragAndDrop(data, mShadow, v, 0);
                        } else {
                            v.startDrag(data, mShadow, null, 0);
                        }
                        break;
                }
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "ACTION_UP");
                switch (v.getId()) {
                    case R.id.room_image_start:
                        Log.d(TAG, "room_image_start");

                        image_start.setPressed(false);
                        if (nReadyCount != 4) {
                            Toast.makeText(getApplicationContext(), "모든 유저가 준비완료 되어야 합니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "시작하기 (셔플)", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case R.id.room_image_ban:
                        Log.d(TAG, "room_image_ban");

                        image_ban.setPressed(false);
                        Toast.makeText(getApplicationContext(), "강퇴시킬 유저를 끌어다놓으세요.", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.room_image_exit:
                        Log.d(TAG, "room_image_exit");

                        image_exit.setPressed(false);
                        Toast.makeText(getApplicationContext(), "방 나가기", Toast.LENGTH_SHORT).show();
                        doExit();
                        break;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
//        String clipData = event.getClipDescription().getLabel().toString();
        ImageView target = (ImageView)v;
        String targetTag = target.getTag().toString();

//        ImageView selected = (ImageView) event.getLocalState();
//        String selectedTag = selected.getTag().toString();
        ImageView selected;
        String selectedTag;

        String targetStr = "";

//        if (!target.getTag().equals("AVATAR")) {
//            Log.d(TAG, "onDrag return false (not avatar) : " + target.getTag());
//            return false;
//        }
//        target.setBackgroundColor(getResources().getColor(R.color.green));



//        if (!selected.getTag().equals("BUTTON")) {
//            Log.d(TAG, "onDrag return false (not button) : " + selected.getTag());
//            return false;
//        }

        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                return true;

            case DragEvent.ACTION_DRAG_LOCATION:
                return true;

            case DragEvent.ACTION_DRAG_ENTERED:
                Log.d(TAG, "ACTION_DRAG_ENTERED : " + targetTag);

                if (target == selectedAvatar) {
                    return false;
                }

                if (targetTag.equals("AVATAR")) {
                    String clipData = event.getClipDescription().getLabel().toString();
                    Log.d(TAG, "clipData : " + clipData);
                    switch (clipData) {
                        case "AVATAR":
//                            target.setBackgroundColor(getResources().getColor(R.color.green));
                            target.setBackgroundColor(Color.parseColor("#8000de6a"));
                            break;
                    }
                    v.invalidate();
                } else if (targetTag.equals("BUTTON")){
                    switch (target.getId()) {
                        case R.id.room_image_ban:
                            target.setPressed(true);
                            break;
                    }
                }
                return true;

            case DragEvent.ACTION_DRAG_EXITED:
                Log.d(TAG, "ACTION_DRAG_EXITED : " + targetTag);

                if (targetTag.equals("AVATAR")) {
                    ClipDescription clipDesc = (ClipDescription)event.getClipDescription();
                    if (clipDesc == null) {
                        return false;
                    }

                    String clipData = clipDesc.getLabel().toString();
                    switch (clipData) {
                        case "AVATAR":
                            target.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                            break;
                    }
                } else if (targetTag.equals("BUTTON")){
                    switch (target.getId()) {
                        case R.id.room_image_ban:
                            target.setPressed(false);
                            break;
                    }
                }
                return true;

            case DragEvent.ACTION_DRAG_ENDED:
                Log.d(TAG, "ACTION_DRAG_ENDED : " + targetTag);

                if (targetTag.equals("AVATAR")) {
                    ClipDescription clipDesc = (ClipDescription)event.getClipDescription();
                    if (clipDesc == null) {
                        Log.d(TAG, "clipDesc is null");
                        return false;
                    }

                    String clipData = clipDesc.getLabel().toString();
                    switch (clipData) {
                        case "AVATAR":
                            target.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                            break;
                    }
                } else if (targetTag.equals("BUTTON")){
                    switch (target.getId()) {
                        case R.id.room_image_ban:
                            target.setPressed(false);
                            break;
                    }
                }
                return true;

            case DragEvent.ACTION_DROP:
                // selected가 선택된 놈, target이 드랍된 놈
                selected = (ImageView) event.getLocalState();
                selectedTag = selected.getTag().toString();

                if (target == selected) {
                    selected.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                    return false;
                }

                Log.d(TAG, "ACTION_DROP (targetTag : " + targetTag + ", selectedTag : " + selectedTag + ")");

                target.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                selected.setBackgroundColor(getResources().getColor(android.R.color.transparent));

                if (targetTag.equals("AVATAR")) {
                    doChange(selected, target);
                } else if (targetTag.equals("BUTTON")) {
                    switch (target.getId()) {
                        case R.id.room_image_ban:
                            doBan(selected);
                            break;
                    }
                } else {
                    Log.d(TAG, "ACTION_DROP else");
                }
                return true;

            default:
                return false;
        }
    }


    private void initListener() {
        image_start.setOnTouchListener(this);
        image_ban.setOnTouchListener(this);
        image_exit.setOnTouchListener(this);

        image_start.setOnDragListener(this);
        image_ban.setOnDragListener(this);
        image_exit.setOnDragListener(this);

        image_avatar_1.setOnTouchListener(this);
        image_avatar_2.setOnTouchListener(this);
        image_avatar_3.setOnTouchListener(this);
        image_avatar_4.setOnTouchListener(this);

        image_avatar_1.setOnDragListener(this);
        image_avatar_2.setOnDragListener(this);
        image_avatar_3.setOnDragListener(this);
        image_avatar_4.setOnDragListener(this);
    }


    private void initRoom() {
        // TODO : 유저 수는 방 설정 값에 따라 결정
        nUserCount = 4;
    }


    private void initUser() {

        // TODO: 방설정과 개인설정 값을 조회할수 있어야 함

        // id : host는 0, client는 host가 지정한 id
        int id = 0;
        // seatNo : host는 1의 위치, client는 host가 지정한 위치
        int seatNo = 1;

        // avatarId, name : 개인 설정 값에서 조회
        int avatarId = 1;
        String name = "USER ";

//        currentUser = new User(id, bHost, seatNo, avatarId, name);

        allUser = new User[nUserCount+1];
        for (int i = 1; i < nUserCount+1; i++) {
            allUser[i] = new User(i, bHost, i, i, name + i);
            bHost = false;
        }
        bHost = true; // 방장

//        currentUser = allUser[1];
//        Log.d(TAG, currentUser.toString());
    }


    private void drawUser(User user) {
        // seatNo
        int seat = user.getSeat();

        int imageId = getResourceId("id", "room_avatar_" + seat);
        int nameId = getResourceId("id", "room_name_" + seat);

        ImageView imageView = (ImageView) findViewById(imageId);
        TextView textView = (TextView) findViewById(nameId);

        // 아바타 표시
        int avatarId = user.getAvatar();
        if (avatarId != 0) {
            imageView.setImageResource(getResourceId("drawable", "avatar_" + avatarId));
        } else {
            imageView.setImageResource(0);
        }

        // 이름 표시
        String name = user.getName();
        textView.setText(name);
    }


    private int getResourceId(String type, String name) {
        // use case
//        // image from res/drawable
//        int resID = getResources().getIdentifier("my_image", "drawable", getPackageName());
//        // view
//        int resID = getResources().getIdentifier("my_resource", "id", getPackageName());
//        // string
//        int resID = getResources().getIdentifier("my_string", "string", getPackageName());
        return getResources().getIdentifier(name, type, getPackageName());
    }


    private void doExit() {
        Log.d(TAG, "doExit");
        // TODO : 로비로 이동하도록 변경
        Intent intent = new Intent(RoomActivity.this, MainActivity.class);
        startActivity(intent);
    }

    private boolean isHost(ImageView selected) {
        boolean ret = false;
        int selectedSeat = getSeatNo(selected);
        ret = allUser[selectedSeat].isHost() == true ? true : false;
        Log.d(TAG, "isHost : " + ret);
        return ret;
    }

    private boolean isExist(ImageView selected) {
        boolean ret = false;
        int selectedSeat = getSeatNo(selected);
        ret = allUser[selectedSeat].getAvatar() != 0 ? true : false;
        Log.d(TAG, "isExist : " + ret);
        return ret;
    }

    private void doChange(ImageView selected, ImageView target) {
        Log.d(TAG, "doChange");

        int selectedSeat = getSeatNo(selected);
        int targetSeat = getSeatNo(target);
        Log.d(TAG, "selectedSeat : " + selectedSeat);
        Log.d(TAG, "targetSeat : " + targetSeat);

        UserWrapper wrapper1 = new UserWrapper(allUser[selectedSeat]);
        UserWrapper wrapper2 = new UserWrapper(allUser[targetSeat]);

        Log.d(TAG, "변경 전");
        Log.d(TAG, allUser[selectedSeat].toString());
        Log.d(TAG, allUser[targetSeat].toString());

        swap(wrapper1, wrapper2);
        allUser[selectedSeat] = wrapper1.user;
        allUser[targetSeat] = wrapper2.user;

        Log.d(TAG, "변경 후");
        Log.d(TAG, allUser[selectedSeat].toString());
        Log.d(TAG, allUser[targetSeat].toString());

        // seat만 변경되는 것이므로 seat값은 재지정
        allUser[selectedSeat].setSeat(selectedSeat);
        allUser[targetSeat].setSeat(targetSeat);

        drawUser(allUser[selectedSeat]);
        drawUser(allUser[targetSeat]);

        Toast.makeText(getApplicationContext(), "자리 변경 완료", Toast.LENGTH_SHORT).show();
    }

    private int getSeatNo(View v) {
        int retSeat = 0;
        switch (v.getId()) {
            case R.id.room_avatar_1: retSeat = 1; break;
            case R.id.room_avatar_2: retSeat = 2; break;
            case R.id.room_avatar_3: retSeat = 3; break;
            case R.id.room_avatar_4: retSeat = 4; break;
        }
        return retSeat;
    }


    private void swap(UserWrapper user1, UserWrapper user2)
    {
        User temp = user1.user;
        user1.user = user2.user;
        user2.user = temp;
    }


    private void doReady(ImageView selected) {
        Log.d(TAG, "doReady");
        Toast.makeText(getApplicationContext(), "준비", Toast.LENGTH_SHORT).show();
    }

    private void doBan(ImageView selected) {
        Log.d(TAG, "doBan");
        int selectedSeat = getSeatNo(selected);
        User user = allUser[selectedSeat];
        if (!user.getName().equals("")) {
            if (user.isHost()) {
                Toast.makeText(getApplicationContext(), "방장은 나갈수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            user.doBan();
            drawUser(user);
            Toast.makeText(getApplicationContext(), "내보내기", Toast.LENGTH_SHORT).show();
        }
    }

    public void doEnterUser(String name, int seatNo, int avatar) {
        Log.d(TAG, "User 입장");


//        "name" : "쫑미니",
//                "seatNo" : 3,
//                "avatar" : 4

    }
}
