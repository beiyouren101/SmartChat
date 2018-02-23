package com.example.jakera.smartchat.Activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.jakera.smartchat.Adapter.ChatRecyclerViewAdapter;
import com.example.jakera.smartchat.Entry.BaseMessageEntry;
import com.example.jakera.smartchat.Entry.TextMessageEntry;
import com.example.jakera.smartchat.Entry.VoiceMessageEntry;
import com.example.jakera.smartchat.Interface.ItemClickListener;
import com.example.jakera.smartchat.R;
import com.example.jakera.smartchat.Utils.MediaManager;
import com.example.jakera.smartchat.Utils.OkhttpHelper;
import com.example.jakera.smartchat.Utils.SpeechSynthesizerUtil;
import com.example.jakera.smartchat.Utils.TranslateUtil;
import com.example.jakera.smartchat.Views.AudioRecorderButton;
import com.youdao.sdk.ydtranslate.Translate;
import com.youdao.sdk.ydtranslate.TranslateErrorCode;
import com.youdao.sdk.ydtranslate.TranslateListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.android.api.content.CustomContent;
import cn.jpush.im.android.api.content.EventNotificationContent;
import cn.jpush.im.android.api.content.ImageContent;
import cn.jpush.im.android.api.content.MessageContent;
import cn.jpush.im.android.api.content.TextContent;
import cn.jpush.im.android.api.content.VoiceContent;
import cn.jpush.im.android.api.event.ConversationRefreshEvent;
import cn.jpush.im.android.api.event.MessageEvent;
import cn.jpush.im.android.api.event.OfflineMessageEvent;
import cn.jpush.im.android.api.model.Conversation;
import cn.jpush.im.android.api.model.Message;
import cn.jpush.im.android.api.model.UserInfo;
import cn.jpush.im.android.api.options.MessageSendingOptions;
import cn.jpush.im.android.tasks.GetEventNotificationTaskMng;
import cn.jpush.im.api.BasicCallback;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by jakera on 18-2-1.
 */

public class ChatActivity extends AppCompatActivity implements Callback,ItemClickListener{

    private String TAG="ChatActivity";

    private RecyclerView recyclerView;
    private List<BaseMessageEntry> datas;
    private ChatRecyclerViewAdapter adapter;

    private TextView tv_send;
    private EditText et_input_text;

    private OkhttpHelper okhttpHelper;

    private ImageButton btn_voice_chat, btn_btn_select_language;

    private AudioRecorderButton mAudioRecorderButton;

    private boolean isVoiceMode=false;

    private boolean isChinese = true;

    private String friendUsername;

    private Conversation conversation;


    private TextView tv_title_bar_center;
    private ImageView iv_title_bar_back;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        //取消设置透明状态栏,使 ContentView 内容不再覆盖状态栏
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //需要设置这个 flag 才能调用 setStatusBarColor 来设置状态栏颜色
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        //设置状态栏颜色
        window.setStatusBarColor(Color.BLACK);
        getSupportActionBar().hide();


        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("username");
        friendUsername = bundle.getString("username");
        setContentView(R.layout.activity_chat);

        init();
        mAudioRecorderButton=(AudioRecorderButton)findViewById(R.id.id_recorder_button);
        mAudioRecorderButton.setAudioFinishRecorderListener(new AudioRecorderButton.AudioFinishRecorderListener() {
            @Override
            public void onFinish(float seconds, String filePath) {
                VoiceMessageEntry voiceMessageEntry=new VoiceMessageEntry(seconds,filePath);
                Log.i(TAG,filePath);
                voiceMessageEntry.setPortrait(BitmapFactory.decodeResource(getResources(),R.mipmap.icon));
                voiceMessageEntry.setViewType(BaseMessageEntry.SENDMESSAGE);
                datas.add(voiceMessageEntry);
                adapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(datas.size()-1);
            }
        });
        recyclerView=(RecyclerView)findViewById(R.id.recyclerview_chat);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter=new ChatRecyclerViewAdapter();
        adapter.setOnItemClickListener(this);

        datas=new ArrayList<>();

        okhttpHelper=new OkhttpHelper();
        okhttpHelper.setCallback(this);

        TextMessageEntry messageEntry0=new TextMessageEntry();
        messageEntry0.setPortrait(BitmapFactory.decodeResource(getResources(),R.drawable.robot_portrait));
        messageEntry0.setContent("嗨，我是小智，来和我聊天吧！！！");
        messageEntry0.setViewType(TextMessageEntry.RECEIVEMESSAGE);

        et_input_text=(EditText)findViewById(R.id.et_input_text);
        tv_send=(TextView)findViewById(R.id.tv_send);
        tv_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextMessageEntry messageEntry=new TextMessageEntry();
                messageEntry.setPortrait(BitmapFactory.decodeResource(getResources(),R.mipmap.icon));
                messageEntry.setContent(et_input_text.getText().toString());
                if (friendUsername.equals(getString(R.string.app_name))) {
                    okhttpHelper.postToTuLingRobot(et_input_text.getText().toString(), "123456");
                }
                messageEntry.setViewType(TextMessageEntry.SENDMESSAGE);
                datas.add(messageEntry);
                adapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(datas.size()-1);


                // JMessageClient.createSingleTextMessage("mary",null,"你好啊");
                MessageContent content = new TextContent(et_input_text.getText().toString());
                //创建一条消息
                Message message = conversation.createSendMessage(content);
                message.setOnSendCompleteCallback(new BasicCallback() {
                    @Override
                    public void gotResult(int i, String s) {
                        Log.i(TAG, "发送结果: i=" + i + ",s=" + s);
                    }
                });
                MessageSendingOptions options = new MessageSendingOptions();
                options.setRetainOffline(false);
                //发送消息
                JMessageClient.sendMessage(message);


                et_input_text.setText("");


            }
        });


        btn_voice_chat=(ImageButton)findViewById(R.id.btn_voice_chat);
        btn_voice_chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isVoiceMode){
                    et_input_text.setVisibility(View.VISIBLE);
                    mAudioRecorderButton.setVisibility(View.GONE);
                    isVoiceMode=!isVoiceMode;
                }else {
                    et_input_text.setVisibility(View.GONE);
                    mAudioRecorderButton.setVisibility(View.VISIBLE);
                    isVoiceMode=!isVoiceMode;
                    //关闭弹出的键盘
                    closeKeyboard();
                }

            }
        });

        btn_btn_select_language = (ImageButton) findViewById(R.id.btn_select_language);
        btn_btn_select_language.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isChinese) {
                    isChinese = false;
                    btn_btn_select_language.setBackground(getResources().getDrawable(R.drawable.english));
                } else {
                    isChinese = true;
                    btn_btn_select_language.setBackground(getResources().getDrawable(R.drawable.chinese));
                }
            }
        });
        if (friendUsername.equals(getString(R.string.app_name))) {
            datas.add(messageEntry0);
        }


        adapter.setDatas(datas);
        recyclerView.setAdapter(adapter);
    }

    public void init() {
        //创建跨应用会话
        conversation = Conversation.createSingleConversation(friendUsername, null);
        tv_title_bar_center = (TextView) findViewById(R.id.tv_title_bar_center);
        tv_title_bar_center.setText(friendUsername);
        iv_title_bar_back = (ImageView) findViewById(R.id.iv_title_bar_back);
        iv_title_bar_back.setVisibility(View.VISIBLE);
        iv_title_bar_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

    }

    @Override
    public void onFailure(Call call, IOException e) {

    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        String answer=response.body().string();
        TextMessageEntry messageEntry=new TextMessageEntry();
        messageEntry.setPortrait(BitmapFactory.decodeResource(getResources(),R.drawable.robot_portrait));
        messageEntry.setContent(okhttpHelper.parseTuLingResult(answer));
        messageEntry.setViewType(TextMessageEntry.RECEIVEMESSAGE);
        datas.add(messageEntry);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(datas.size()-1);
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        MediaManager.pause();
        JMessageClient.unRegisterEventReceiver(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MediaManager.resume();
        JMessageClient.registerEventReceiver(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MediaManager.release();
    }

    @Override
    public void OnItemClick(View v, final int position) {
        if(datas.get(position) instanceof VoiceMessageEntry){
            MediaManager.playSound(((VoiceMessageEntry) datas.get(position)).getFilePath(),null);
        } else if (datas.get(position) instanceof TextMessageEntry) {
            String fromLanguage, toLanguage;
            if (isChinese) {
                fromLanguage = "英文";
                toLanguage = "中文";
            } else {
                fromLanguage = "中文";
                toLanguage = "英文";
            }
            String content = ((TextMessageEntry) datas.get(position)).getContent();
            TranslateUtil.translate(fromLanguage, toLanguage, content, new TranslateListener() {
                @Override
                public void onError(TranslateErrorCode translateErrorCode, String s) {

                }

                @Override
                public void onResult(Translate translate, String s, String s1) {
                    if (translate.getTranslations().size() > 0) {
                        ((TextMessageEntry) datas.get(position)).setContent(translate.getTranslations().get(0));
                        SpeechSynthesizerUtil.getInstance().speakText(translate.getTranslations().get(0));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                            }
                        });

                    }
                }
            });
        }
    }

    //不同的Event接收不用的实体对象
    public void onEvent(MessageEvent event) {
        Message msg = event.getMessage();
        UserInfo freind = msg.getFromUser();
        String username_receiver = freind.getUserName();
        Log.i(TAG, "onEvent:接到事件");

        switch (msg.getContentType()) {
            case text:
                //处理文字消息
                TextContent textContent = (TextContent) msg.getContent();
                textContent.getText();
                Log.i(TAG, username_receiver + ":" + textContent.getText());
                break;
            case image:
                //处理图片消息
                ImageContent imageContent = (ImageContent) msg.getContent();
                imageContent.getLocalPath();//图片本地地址
                imageContent.getLocalThumbnailPath();//图片对应缩略图的本地地址
                break;
            case voice:
                //处理语音消息
                VoiceContent voiceContent = (VoiceContent) msg.getContent();
                voiceContent.getLocalPath();//语音文件本地地址
                voiceContent.getDuration();//语音文件时长
                break;
            case custom:
                //处理自定义消息
                CustomContent customContent = (CustomContent) msg.getContent();
                customContent.getNumberValue("custom_num"); //获取自定义的值
                customContent.getBooleanValue("custom_boolean");
                customContent.getStringValue("custom_string");
                break;
            case eventNotification:
                //处理事件提醒消息
                EventNotificationContent eventNotificationContent = (EventNotificationContent) msg.getContent();
                switch (eventNotificationContent.getEventNotificationType()) {
                    case group_member_added:
                        //群成员加群事件
                        break;
                    case group_member_removed:
                        //群成员被踢事件
                        break;
                    case group_member_exit:
                        //群成员退群事件
                        break;
                    case group_info_updated://since 2.2.1
                        //群信息变更事件
                        break;
                }
                break;
        }
    }


    /**
     * 类似MessageEvent事件的接收，上层在需要的地方增加OfflineMessageEvent事件的接收
     * 即可实现离线消息的接收。
     **/
    public void onEvent(OfflineMessageEvent event) {
        //获取事件发生的会话对象
        Log.i(TAG, "收到消息");
        Conversation conversation = event.getConversation();
        List<Message> newMessageList = event.getOfflineMessageList();//获取此次离线期间会话收到的新消息列表
        System.out.println(String.format(Locale.SIMPLIFIED_CHINESE, "收到%d条来自%s的离线消息。\n", newMessageList.size(), conversation.getTargetId()));
        for (int i = 0; i < newMessageList.size(); i++) {
            //  {"text":"你好","extras":{}}
//            Log.i(TAG,"i="+i+","+newMessageList.get(i).getContent().toJson());
//            Log.i(TAG,"i="+i+","+newMessageList.get(i).getContent().);

        }


    }


    /**
     * 如果在JMessageClient.init时启用了消息漫游功能，则每当一个会话的漫游消息同步完成时
     * sdk会发送此事件通知上层。
     **/
    public void onEvent(ConversationRefreshEvent event) {
        Log.i(TAG, "收到消息");
        //获取事件发生的会话对象
        Conversation conversation = event.getConversation();
        //获取事件发生的原因，对于漫游完成触发的事件，此处的reason应该是
        //MSG_ROAMING_COMPLETE
        ConversationRefreshEvent.Reason reason = event.getReason();
        System.out.println(String.format(Locale.SIMPLIFIED_CHINESE, "收到ConversationRefreshEvent事件,待刷新的会话是%s.\n", conversation.getTargetId()));
        System.out.println("事件发生的原因 : " + reason);
    }


    private void closeKeyboard() {
        View view = getWindow().peekDecorView();
        if (view != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
