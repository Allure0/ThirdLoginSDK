package com.allure.thirdlogin.weixin;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.allure.thirdlogin.PlatformConfig;
import com.allure.thirdlogin.PlatformType;
import com.allure.thirdlogin.SSOHandler;
import com.allure.thirdlogin.listener.AuthListener;
import com.allure.thirdlogin.listener.ShareListener;
import com.allure.thirdlogin.share_media.IShareMedia;
import com.allure.thirdlogin.share_media.ShareImageMedia;
import com.allure.thirdlogin.share_media.ShareMusicMedia;
import com.allure.thirdlogin.share_media.ShareTextMedia;
import com.allure.thirdlogin.share_media.ShareVideoMedia;
import com.allure.thirdlogin.share_media.ShareWebMedia;
import com.allure.thirdlogin.util.BitmapUtils;
import com.allure.thirdlogin.util.LogUtils;
import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXMusicObject;
import com.tencent.mm.opensdk.modelmsg.WXTextObject;
import com.tencent.mm.opensdk.modelmsg.WXVideoObject;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信处理Handler
 * Created by tsy on 16/8/4.
 */
public class WXHandler extends SSOHandler {

    private Context mContext;
    private Activity mActivtiy;

    private IWXAPI mWXApi;

    //默认scope 和 state
    private static String sScope = "snsapi_userinfo,snsapi_friend,snsapi_message";
    private static String sState = "none";

    private IWXAPIEventHandler mEventHandler;
    private String mLastTransaction = "";

    private PlatformConfig.Weixin mConfig;
    private AuthListener mAuthListener;
    private ShareListener mShareListener;

    public WXHandler() {
        this.mEventHandler = new IWXAPIEventHandler() {
            public void onResp(BaseResp resp) {
                if(!mLastTransaction.equals(resp.transaction)) {
                    return;
                }

                int type = resp.getType();
                switch(type) {
                    case ConstantsAPI.COMMAND_SENDAUTH:     //授权返回
                        WXHandler.this.onAuthCallback((SendAuth.Resp)resp);
                        break;

                    case ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX:        //分享返回
                        WXHandler.this.onShareCallback((SendMessageToWX.Resp)resp);
                        break;
                }

            }

            public void onReq(BaseReq req) {
            }
        };
    }

    /**
     * 设置scope和state
     * @param scope
     * @param state
     */
    public static void setScopeState(String scope, String state) {
        sScope = scope;
        sState = state;
    }

    @Override
    public void onCreate(Context context, PlatformConfig.Platform config) {
        this.mContext = context;
        this.mConfig = (PlatformConfig.Weixin) config;

        this.mWXApi = WXAPIFactory.createWXAPI(mContext.getApplicationContext(), this.mConfig.appId);
        this.mWXApi.registerApp(this.mConfig.appId);
    }

    @Override
    public boolean isInstall() {
        return this.mWXApi.isWXAppInstalled();
    }

    @Override
    public void authorize(Activity activity, AuthListener authListener) {
        if(!isInstall()) {
            authListener.onError(this.mConfig.getName(), "wx not install");
            LogUtils.e("wx not install");
            return ;
        }

        this.mActivtiy = activity;
        this.mAuthListener = authListener;

        SendAuth.Req req1 = new SendAuth.Req();
        req1.scope = sScope;
        req1.state = sState;
        req1.transaction = buildTransaction("authorize");
        mLastTransaction = req1.transaction;

        if(!this.mWXApi.sendReq(req1)) {
            this.mAuthListener.onError(this.mConfig.getName(), "sendReq fail");
            LogUtils.e("wxapi sendReq fail");
        }
    }

    //验证回调
    protected void onAuthCallback(SendAuth.Resp resp) {
        switch (resp.errCode) {
            case BaseResp.ErrCode.ERR_OK:       //授权成功
                Map<String, String> data = new HashMap<String, String>();
                data.put("code", resp.code);
                this.mAuthListener.onComplete(PlatformType.WEIXIN, data);
                break;

            case BaseResp.ErrCode.ERR_USER_CANCEL:      //授权取消
                if(this.mAuthListener != null) {
                    this.mAuthListener.onCancel(PlatformType.WEIXIN);
                }
                break;

            default:    //授权失败
                CharSequence err = TextUtils.concat(new CharSequence[]{"weixin auth error (", String.valueOf(resp.errCode), "):", resp.errStr});
                if(mAuthListener != null) {
                    mAuthListener.onError(PlatformType.WEIXIN, err.toString());
                }
                break;
        }
    }

    @Override
    public void share(Activity activity, IShareMedia shareMedia, ShareListener shareListener) {
        if(!isInstall()) {
            shareListener.onError(this.mConfig.getName(), "wx not install");
            LogUtils.e("wx not install");
            return ;
        }

        this.mActivtiy = activity;
        this.mShareListener = shareListener;

        WXMediaMessage msg = new WXMediaMessage();
        String type = "";

        if(shareMedia instanceof ShareWebMedia) {       //网页分享
            ShareWebMedia shareWebMedia = (ShareWebMedia) shareMedia;
            type = "webpage";

            //web object
            WXWebpageObject webpageObject = new WXWebpageObject();
            webpageObject.webpageUrl = shareWebMedia.getWebPageUrl();

            msg.mediaObject = webpageObject;
            msg.title = shareWebMedia.getTitle();
            msg.description = shareWebMedia.getDescription();
            msg.thumbData = BitmapUtils.bitmap2Bytes(shareWebMedia.getThumb());
        } else if(shareMedia instanceof ShareTextMedia) {   //文字分享
            ShareTextMedia shareTextMedia = (ShareTextMedia) shareMedia;
            type = "text";

            //text object
            WXTextObject textObject = new WXTextObject();
            textObject.text = shareTextMedia.getText();

            msg.mediaObject = textObject;
            msg.description = shareTextMedia.getText();
        } else if(shareMedia instanceof ShareImageMedia) {  //图片分享
            ShareImageMedia shareImageMedia = (ShareImageMedia) shareMedia;
            type = "image";

            //image object
            WXImageObject imageObject = new WXImageObject();
            //image限制10M
            imageObject.imageData = BitmapUtils.compressBitmap(BitmapUtils.bitmap2Bytes(shareImageMedia.getImage()), 10 * 1024 * 1024);

            msg.mediaObject = imageObject;

            //直接缩放图片
            Bitmap thumb = Bitmap.createScaledBitmap(shareImageMedia.getImage(), 200, 200, true);
            msg.thumbData = BitmapUtils.bitmap2Bytes(thumb);
            thumb.recycle();
        } else if(shareMedia instanceof ShareMusicMedia) {  //音乐分享
            ShareMusicMedia shareMusicMedia = (ShareMusicMedia) shareMedia;
            type = "music";

            WXMusicObject musicObject = new WXMusicObject();
            musicObject.musicUrl = shareMusicMedia.getMusicUrl();

            msg.mediaObject = musicObject;
            msg.title = shareMusicMedia.getTitle();
            msg.description = shareMusicMedia.getDescription();
            msg.thumbData = BitmapUtils.bitmap2Bytes(shareMusicMedia.getThumb());
        } else if(shareMedia instanceof ShareVideoMedia) {      //视频分享
            ShareVideoMedia shareVideoMedia = (ShareVideoMedia) shareMedia;
            type = "video";

            WXVideoObject videoObject = new WXVideoObject();
            videoObject.videoUrl = shareVideoMedia.getVideoUrl();

            msg.mediaObject = videoObject;
            msg.title = shareVideoMedia.getTitle();
            msg.description = shareVideoMedia.getDescription();
            msg.thumbData = BitmapUtils.bitmap2Bytes(shareVideoMedia.getThumb());
        } else {
            if(this.mShareListener != null) {
                this.mShareListener.onError(this.mConfig.getName(), "weixin is not support this shareMedia");
            }
            return ;
        }

        //压缩缩略图到32kb
        if(msg.thumbData != null && msg.thumbData.length > '耀') {        //微信sdk里面判断的大小
            msg.thumbData = BitmapUtils.compressBitmap(msg.thumbData, '耀');
        }

        //发起request
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.message = msg;
        req.transaction = buildTransaction(type);
        mLastTransaction = req.transaction;

        if(this.mConfig.getName() == PlatformType.WEIXIN) {     //分享好友
            req.scene = SendMessageToWX.Req.WXSceneSession;
        } else if(this.mConfig.getName() == PlatformType.WEIXIN_CIRCLE) {      //分享朋友圈
            req.scene = SendMessageToWX.Req.WXSceneTimeline;
        }

        if(!this.mWXApi.sendReq(req)) {
            if(this.mShareListener != null) {
                this.mShareListener.onError(this.mConfig.getName(), "sendReq fail");
            }
            LogUtils.e("wxapi sendReq fail");
        }
    }

    protected void onShareCallback(SendMessageToWX.Resp resp) {
        switch (resp.errCode) {
            case BaseResp.ErrCode.ERR_OK:       //分享成功
                if(this.mShareListener != null) {
                    this.mShareListener.onComplete(this.mConfig.getName());
                }
                break;

            case BaseResp.ErrCode.ERR_USER_CANCEL:      //分享取消
                if(this.mShareListener != null) {
                    this.mShareListener.onCancel(this.mConfig.getName());
                }
                break;

            default:    //分享失败
                CharSequence err = TextUtils.concat(new CharSequence[]{"weixin share error (", String.valueOf(resp.errCode), "):", resp.errStr});
                if(mShareListener != null) {
                    mShareListener.onError(this.mConfig.getName(), err.toString());
                }
                break;
        }
    }

    private String buildTransaction(String type) {
        return type == null?String.valueOf(System.currentTimeMillis()):type + System.currentTimeMillis();
    }

    public IWXAPI getWXApi() {
        return this.mWXApi;
    }

    public IWXAPIEventHandler getWXEventHandler() {
        return this.mEventHandler;
    }
}
