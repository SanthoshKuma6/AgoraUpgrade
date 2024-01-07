package in.sk.agora.agora.media;
/**
 * Created by santhosh on 20/12/2023.
 */
public class RtcTokenBuilder {
    public enum Role {
        ROLE_PUBLISHER(1),
        ROLE_SUBSCRIBER(2),
        ;

        public final int initValue;

        Role(int initValue) {
            this.initValue = initValue;
        }
    }


    public String buildTokenWithUid(String appId, String appCertificate, String channelName, int uid, Role role, int token_expire, int privilege_expire) {
        return buildTokenWithUserAccount(appId, appCertificate, channelName, AccessToken.getUidStr(uid), role, token_expire, privilege_expire);
    }

    public String buildTokenWithUserAccount(String appId, String appCertificate, String channelName, String account, Role role, int token_expire, int privilege_expire) {
        AccessToken accessToken = new AccessToken(appId, appCertificate, token_expire);
        AccessToken.Service serviceRtc = new AccessToken.ServiceRtc(channelName, account);

        serviceRtc.addPrivilegeRtc(AccessToken.PrivilegeRtc.PRIVILEGE_JOIN_CHANNEL, privilege_expire);
        if (role == Role.ROLE_PUBLISHER) {
            serviceRtc.addPrivilegeRtc(AccessToken.PrivilegeRtc.PRIVILEGE_PUBLISH_AUDIO_STREAM, privilege_expire);
            serviceRtc.addPrivilegeRtc(AccessToken.PrivilegeRtc.PRIVILEGE_PUBLISH_VIDEO_STREAM, privilege_expire);
            serviceRtc.addPrivilegeRtc(AccessToken.PrivilegeRtc.PRIVILEGE_PUBLISH_DATA_STREAM, privilege_expire);
        }
        accessToken.addService(serviceRtc);

        try {
            return accessToken.build();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }


    public String buildTokenWithUid(String appId, String appCertificate, String channelName, int uid,
                                    int tokenExpire, int joinChannelPrivilegeExpire, int pubAudioPrivilegeExpire,
                                    int pubVideoPrivilegeExpire, int pubDataStreamPrivilegeExpire) {
        return buildTokenWithUserAccount(appId, appCertificate, channelName, AccessToken.getUidStr(uid),
                tokenExpire, joinChannelPrivilegeExpire, pubAudioPrivilegeExpire, pubVideoPrivilegeExpire, pubDataStreamPrivilegeExpire);
    }


    public String buildTokenWithUserAccount(String appId, String appCertificate, String channelName, String account,
                                            int tokenExpire, int joinChannelPrivilegeExpire, int pubAudioPrivilegeExpire,
                                            int pubVideoPrivilegeExpire, int pubDataStreamPrivilegeExpire) {
        AccessToken accessToken = new AccessToken(appId, appCertificate, tokenExpire);
        AccessToken.Service serviceRtc = new AccessToken.ServiceRtc(channelName, account);

        serviceRtc.addPrivilegeRtc(AccessToken.PrivilegeRtc.PRIVILEGE_JOIN_CHANNEL, joinChannelPrivilegeExpire);
        serviceRtc.addPrivilegeRtc(AccessToken.PrivilegeRtc.PRIVILEGE_PUBLISH_AUDIO_STREAM, pubAudioPrivilegeExpire);
        serviceRtc.addPrivilegeRtc(AccessToken.PrivilegeRtc.PRIVILEGE_PUBLISH_VIDEO_STREAM, pubVideoPrivilegeExpire);
        serviceRtc.addPrivilegeRtc(AccessToken.PrivilegeRtc.PRIVILEGE_PUBLISH_DATA_STREAM, pubDataStreamPrivilegeExpire);
        accessToken.addService(serviceRtc);

        try {
            return accessToken.build();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
