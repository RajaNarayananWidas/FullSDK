package com.example.cidaasv2.Controller.Repository.Configuration.TOTP;

import android.content.Context;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.example.cidaasv2.Controller.Cidaas;
import com.example.cidaasv2.Controller.Repository.AccessToken.AccessTokenController;
import com.example.cidaasv2.Controller.Repository.Configuration.TOTP.TOTPConfigurationController;
import com.example.cidaasv2.Controller.Repository.Configuration.TOTP.TOTPGenerator.GoogleAuthenticator;
import com.example.cidaasv2.Controller.Repository.Configuration.TOTP.TOTPGenerator.TOTP;
import com.example.cidaasv2.Controller.Repository.Configuration.TOTP.TOTPGenerator.TotpClock;
import com.example.cidaasv2.Controller.Repository.Login.LoginController;
import com.example.cidaasv2.Helper.Enums.Result;
import com.example.cidaasv2.Helper.Enums.UsageType;
import com.example.cidaasv2.Helper.Extension.WebAuthError;
import com.example.cidaasv2.Helper.Genral.DBHelper;
import com.example.cidaasv2.Helper.Logger.LogFile;
import com.example.cidaasv2.Helper.pkce.OAuthChallengeGenerator;
import com.example.cidaasv2.Service.Entity.AccessTokenEntity;
import com.example.cidaasv2.Service.Entity.LoginCredentialsEntity.LoginCredentialsResponseEntity;
import com.example.cidaasv2.Service.Entity.LoginCredentialsEntity.ResumeLogin.ResumeLoginRequestEntity;
import com.example.cidaasv2.Service.Entity.MFA.AuthenticateMFA.TOTP.AuthenticateTOTPRequestEntity;
import com.example.cidaasv2.Service.Entity.MFA.AuthenticateMFA.TOTP.AuthenticateTOTPResponseEntity;
import com.example.cidaasv2.Service.Entity.MFA.EnrollMFA.TOTP.EnrollTOTPMFARequestEntity;
import com.example.cidaasv2.Service.Entity.MFA.EnrollMFA.TOTP.EnrollTOTPMFAResponseEntity;
import com.example.cidaasv2.Service.Entity.MFA.EnrollMFA.TOTP.EnrollTOTPMFARequestEntity;
import com.example.cidaasv2.Service.Entity.MFA.EnrollMFA.TOTP.EnrollTOTPMFAResponseEntity;
import com.example.cidaasv2.Service.Entity.MFA.InitiateMFA.TOTP.InitiateTOTPMFARequestEntity;
import com.example.cidaasv2.Service.Entity.MFA.InitiateMFA.TOTP.InitiateTOTPMFAResponseEntity;
import com.example.cidaasv2.Service.Entity.MFA.SetupMFA.TOTP.SetupTOTPMFARequestEntity;
import com.example.cidaasv2.Service.Entity.MFA.SetupMFA.TOTP.SetupTOTPMFAResponseEntity;
import com.example.cidaasv2.Service.Entity.MFA.SetupMFA.TOTP.SetupTOTPMFARequestEntity;
import com.example.cidaasv2.Service.Entity.MFA.SetupMFA.TOTP.SetupTOTPMFAResponseEntity;
import com.example.cidaasv2.Service.Entity.ValidateDevice.ValidateDeviceResponseEntity;
import com.example.cidaasv2.Service.Repository.OauthService;
import com.example.cidaasv2.Service.Repository.Verification.Device.DeviceVerificationService;
import com.example.cidaasv2.Service.Repository.Verification.TOTP.TOTPVerificationService;
import com.example.cidaasv2.Service.Scanned.ScannedResponseEntity;

import java.text.DecimalFormat;
import java.util.Dictionary;

import timber.log.Timber;

public class TOTPConfigurationController {

    private String authenticationType,secret;
    private String verificationType;
    private Context context;

    public static TOTPConfigurationController shared;

    public TOTPConfigurationController(Context contextFromCidaas) {

        verificationType="";
        context=contextFromCidaas;
        authenticationType="";
        //Todo setValue for authenticationType

    }

    String codeVerifier, codeChallenge;
    // Generate Code Challenge and Code verifier
    public void generateChallenge(){
        OAuthChallengeGenerator generator = new OAuthChallengeGenerator();

        codeVerifier=generator.getCodeVerifier();
        codeChallenge= generator.getCodeChallenge(codeVerifier);

    }

    public static TOTPConfigurationController getShared(Context contextFromCidaas )
    {
        try {

            if (shared == null) {
                shared = new TOTPConfigurationController(contextFromCidaas);
            }
        }
        catch (Exception e)
        {
            Timber.i(e.getMessage());
        }
        return shared;
    }


//Todo Configure TOTP by Passing the setupTOTPRequestEntity
    // 1.  Check For NotNull Values
    // 2. Generate Code Challenge
    // 3.  getAccessToken From Sub
    // 4.  Call Setup TOTP
    // 5.  Call Validate TOTP
    // 3.  Call Scanned TOTP
    // 3.  Call Enroll TOTP and return the result
    // 4.  Maintain logs based on flags


    //Service call To SetupTOTPMFA
    public void configureTOTP(@NonNull final String sub, @NonNull final String baseurl,
                                 @NonNull final SetupTOTPMFARequestEntity setupTOTPMFARequestEntity,
                                 @NonNull final Result<EnrollTOTPMFAResponseEntity> enrollresult)
    {
        try{
            //Generate Challenge
            generateChallenge();
            Cidaas.instanceId="";

            AccessTokenController.getShared(context).getAccessToken(sub, new Result<AccessTokenEntity>()
            {
                @Override
                public void success(final AccessTokenEntity accessTokenresult) {

                    if (baseurl != null && !baseurl.equals("") && accessTokenresult.getAccess_token() != null && !accessTokenresult.getAccess_token().equals("") &&
                            setupTOTPMFARequestEntity.getClient_id()!=null && !setupTOTPMFARequestEntity.getClient_id().equals(""))
                    {
                        //Todo Service call

                        TOTPVerificationService.getShared(context).setupTOTP(baseurl, accessTokenresult.getAccess_token(), codeChallenge,setupTOTPMFARequestEntity,new Result<SetupTOTPMFAResponseEntity>() {
                            @Override
                            public void success(final SetupTOTPMFAResponseEntity setupserviceresult) {

                                String queryString=setupserviceresult.getData().getQueryString();

                                String [] stringArray = queryString.split("&", 2);
                                 secret=stringArray[0];

                                new CountDownTimer(5000, 500) {
                                    String instceID="";
                                    public void onTick(long millisUntilFinished) {
                                        instceID= Cidaas.instanceId;

                                        Timber.e("");
                                        if(instceID!=null && instceID!="")
                                        {
                                            this.cancel();
                                            onFinish();
                                        }

                                    }
                                    public void onFinish() {
                                        if(instceID!=null && instceID!="" && setupserviceresult.getData().getStatusId()!=null && setupserviceresult.getData().getStatusId()!="")
                                        {
                                            //Device Validation Service
                                            DeviceVerificationService.getShared(context).validateDevice(baseurl,instceID,setupserviceresult.getData().getStatusId(),codeVerifier
                                                    , new Result<ValidateDeviceResponseEntity>() {
                                                        @Override
                                                        public void success(ValidateDeviceResponseEntity result) {
                                                            // call Scanned Service
                                                            TOTPVerificationService.getShared(context).scannedTOTP(baseurl,result.getData().getUsage_pass(),setupserviceresult.getData().getStatusId(),
                                                                    accessTokenresult.getAccess_token(),new Result<ScannedResponseEntity>() {
                                                                        @Override
                                                                        public void success(final ScannedResponseEntity result) {
                                                                            DBHelper.getShared().setUserDeviceId(result.getData().getUserDeviceId(),baseurl);

                                                                            EnrollTOTPMFARequestEntity enrollTOTPMFARequestEntity = new EnrollTOTPMFARequestEntity();
                                                                            if(sub != null && !sub.equals("") &&
                                                                                    result.getData().getUserDeviceId()!=null && !  result.getData().getUserDeviceId().equals("")) {

                                                                                String totp="";
                                                                                if(secret!=null) {

                                                                                  totp=  generateTOTP(secret);
                                                                                }
                                                                                enrollTOTPMFARequestEntity.setSub(sub);
                                                                                enrollTOTPMFARequestEntity.setVerifierPassword(totp);
                                                                                enrollTOTPMFARequestEntity.setStatusId(setupserviceresult.getData().getStatusId());
                                                                                enrollTOTPMFARequestEntity.setUserDeviceId(result.getData().getUserDeviceId());
                                                                            }
                                                                            else {
                                                                                enrollresult.failure(WebAuthError.getShared(context).propertyMissingException());
                                                                            }

                                                                            // call Enroll Service
                                                                            TOTPVerificationService.getShared(context).enrollTOTP(baseurl, accessTokenresult.getAccess_token(), enrollTOTPMFARequestEntity,new Result<EnrollTOTPMFAResponseEntity>() {
                                                                                @Override
                                                                                public void success(EnrollTOTPMFAResponseEntity serviceresult) {
                                                                                    enrollresult.success(serviceresult);
                                                                                }

                                                                                @Override
                                                                                public void failure(WebAuthError error) {
                                                                                    enrollresult.failure(error);
                                                                                }
                                                                            });

                                                                            Timber.i(result.getData().getUserDeviceId()+"User Device id");
                                                                            Toast.makeText(context, result.getData().getUserDeviceId()+"User Device id", Toast.LENGTH_SHORT).show();
                                                                        }

                                                                        @Override
                                                                        public void failure(WebAuthError error) {
                                                                            enrollresult.failure(error);
                                                                            Toast.makeText(context, "Error on Scanned"+error.getErrorMessage(), Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    });
                                                        }

                                                        @Override
                                                        public void failure(WebAuthError error) {
                                                            enrollresult.failure(error);
                                                            Toast.makeText(context, "Error on validate Device"+error.getErrorMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                        else {
                                            // return Error Message

                                            enrollresult.failure(WebAuthError.getShared(context).deviceVerificationFailureException());
                                        }
                                    }
                                }.start();


                                // result.success(serviceresult);
                            }

                            @Override
                            public void failure(WebAuthError error) {
                                enrollresult.failure(error);
                            }
                        });
                    }
                    else
                    {

                        enrollresult.failure(WebAuthError.getShared(context).propertyMissingException());
                    }
                }

                @Override
                public void failure(WebAuthError error) {
                    enrollresult.failure(error);
                }
            });

        }
        catch (Exception e)
        {
            enrollresult.failure(WebAuthError.getShared(context).propertyMissingException());
            Timber.e(e.getMessage());
        }
    }



    //Login with TOTP
    public void LoginWithTOTP(@NonNull final String baseurl, @NonNull final String clientId, @NonNull final String trackId, @NonNull final String requestId,
                              @NonNull final InitiateTOTPMFARequestEntity initiateTOTPMFARequestEntity,
                              final Result<LoginCredentialsResponseEntity> loginresult)
    {
        try{

            if(initiateTOTPMFARequestEntity.getUserDeviceId() != null && initiateTOTPMFARequestEntity.getUserDeviceId() != "" )
            {
                //Do nothing
            }
            else
            {
                initiateTOTPMFARequestEntity.setUserDeviceId(DBHelper.getShared().getUserDeviceId(baseurl));
            }

            if (    initiateTOTPMFARequestEntity.getUsageType() != null && initiateTOTPMFARequestEntity.getUsageType() != "" &&
                    initiateTOTPMFARequestEntity.getUserDeviceId() != null && initiateTOTPMFARequestEntity.getUserDeviceId() != ""&&
                    initiateTOTPMFARequestEntity.getSub() != null && initiateTOTPMFARequestEntity.getSub() != "" &&
                    initiateTOTPMFARequestEntity.getEmail() != null && initiateTOTPMFARequestEntity.getEmail() != ""&&
                    baseurl != null && !baseurl.equals("")) {
                //Todo Service call
                TOTPVerificationService.getShared(context).initiateTOTP(baseurl, initiateTOTPMFARequestEntity,
                        new Result<InitiateTOTPMFAResponseEntity>() {

                            //Todo Call Validate Device

                            @Override
                            public void success(InitiateTOTPMFAResponseEntity serviceresult) {
                                if (requestId != null && !requestId.equals("") && serviceresult.getData().getStatusId() != null &&
                                        !serviceresult.getData().getStatusId().equals("")) {


                                    AuthenticateTOTPRequestEntity authenticateTOTPRequestEntity = new AuthenticateTOTPRequestEntity();
                                    authenticateTOTPRequestEntity.setUserDeviceId(initiateTOTPMFARequestEntity.getUserDeviceId());
                                    authenticateTOTPRequestEntity.setStatusId(serviceresult.getData().getStatusId());
                                    String totp=generateTOTP(secret);

                                    authenticateTOTPRequestEntity.setVerifierPassword(totp);


                                    TOTPVerificationService.getShared(context).authenticateTOTP(baseurl, authenticateTOTPRequestEntity, new Result<AuthenticateTOTPResponseEntity>() {
                                        @Override
                                        public void success(AuthenticateTOTPResponseEntity result) {
                                            //Todo Call Resume with Login Service
                                            //  loginresult.success(result);
                                            Toast.makeText(context, "Sucess TOTP", Toast.LENGTH_SHORT).show();
                                            ResumeLoginRequestEntity resumeLoginRequestEntity=new ResumeLoginRequestEntity();

                                            //Todo Check not Null values
                                            resumeLoginRequestEntity.setSub(result.getData().getSub());
                                            resumeLoginRequestEntity.setTrackingCode(result.getData().getTrackingCode());
                                            resumeLoginRequestEntity.setUsageType(result.getData().getUsageType());
                                            resumeLoginRequestEntity.setVerificationType(result.getData().getVerificationType());
                                            resumeLoginRequestEntity.setClient_id(clientId);
                                            resumeLoginRequestEntity.setRequestId(requestId);

                                            if(initiateTOTPMFARequestEntity.getUsageType().equals(UsageType.MFA))
                                            {
                                                resumeLoginRequestEntity.setTrack_id(trackId);
                                                LoginController.getShared(context).continueMFA(baseurl,resumeLoginRequestEntity,loginresult);
                                            }

                                            else if(initiateTOTPMFARequestEntity.getUsageType().equals(UsageType.PASSWORDLESS))
                                            {
                                                resumeLoginRequestEntity.setTrack_id("");
                                                LoginController.getShared(context).continuePasswordless(baseurl,resumeLoginRequestEntity,loginresult);

                                            }
                                        }

                                        @Override
                                        public void failure(WebAuthError error) {
                                            loginresult.failure(error);
                                        }
                                    });
                                    // result.success(serviceresult);
                                }
                            }

                            @Override
                            public void failure(WebAuthError error) {
                                loginresult.failure(error);
                            }
                        });
            }
            else
            {

                loginresult.failure(WebAuthError.getShared(context).propertyMissingException());
            }
        }
        catch (Exception e)
        {
            Timber.e(e.getMessage());
        }
    }



    private String generateTOTP(String secret)
    {
        String TOTP="";
        try
        {
            TotpClock totpClock;
            int local_totp, local_totp1;



            totpClock = new TotpClock(context);
            long temp = totpClock.currentTimeMillis();
            local_totp = (int)((temp / 1000) % 30);
            long temp1 = temp - 1000;
            local_totp1 = (int)((temp1 / 100) % 300);

            // set progress state

            DecimalFormat format = new DecimalFormat("00");
            String formattedResult = format.format(30 - local_totp);



            if(local_totp == 0)
            {
                TOTP= GoogleAuthenticator.getTOTPCode(secret);
            }

            return TOTP;
        }
        catch (Exception e) {
          return TOTP;
        }
    }




   /* //setupTOTPMFA
    public void setupTOTPMFA(@NonNull String sub, @NonNull final Result<SetupTOTPMFAResponseEntity> result){
        try {
            String baseurl="";
            if(savedProperties==null){

                savedProperties= DBHelper.getShared().getLoginProperties();
            }
            if(savedProperties==null){
                //Read from file if localDB is null
                readFromFile(new Result<Dictionary<String, String>>() {
                    @Override
                    public void success(Dictionary<String, String> loginProperties) {
                        savedProperties=loginProperties;
                    }

                    @Override
                    public void failure(WebAuthError error) {
                        result.failure(error);
                    }
                });
            }

            if (savedProperties.get("DomainURL").equals("") || savedProperties.get("DomainURL") == null || savedProperties == null) {
                webAuthError = webAuthError.propertyMissingException();
                String loggerMessage = "Setup TOTP MFA readProperties failure : " + "Error Code - " + webAuthError.errorCode + ", Error Message - " + webAuthError.ErrorMessage
                        + ", Status Code - " + webAuthError.statusCode;
                LogFile.addRecordToLog(loggerMessage);
                result.failure(webAuthError);
            } if (savedProperties.get("ClientId").equals("") || savedProperties.get("ClientId") == null || savedProperties == null) {
                webAuthError = webAuthError.propertyMissingException();
                String loggerMessage = "Accept Consent readProperties failure : " + "Error Code - " + webAuthError.errorCode + ", Error Message - " + webAuthError.ErrorMessage
                        + ", Status Code - " + webAuthError.statusCode;
                LogFile.addRecordToLog(loggerMessage);
                result.failure(webAuthError);
            }
            else {
                baseurl = savedProperties.get("DomainURL");

                if ( sub != null && !sub.equals("") && baseurl != null && !baseurl.equals("")) {

                    final String finalBaseurl = baseurl;
                    getAccessToken(sub, new Result<AccessTokenEntity>() {
                        @Override
                        public void success(AccessTokenEntity accesstokenresult) {

                            String logoUrl= "https://docs.cidaas.de/assets/logoss.png";



                            SetupTOTPMFARequestEntity setupTOTPMFARequestEntity=new SetupTOTPMFARequestEntity();
                            setupTOTPMFARequestEntity.setClient_id( savedProperties.get("ClientId"));
                            setupTOTPMFARequestEntity.setLogoUrl(logoUrl);


                            setupTOTPMFAService(accesstokenresult.getAccess_token(), finalBaseurl,setupTOTPMFARequestEntity,result);
                        }

                        @Override
                        public void failure(WebAuthError error) {
                            result.failure(error);
                        }
                    });


                }

            }


        }
        catch (Exception e)
        {
            LogFile.addRecordToLog("acceptConsent exception"+e.getMessage());
            Timber.e("acceptConsent exception"+e.getMessage());
        }
    }

    //Service call To SetupTOTPMFA
    private void setupTOTPMFAService(@NonNull final String AccessToken, @NonNull String baseurl,
                                     @NonNull SetupTOTPMFARequestEntity setupTOTPMFARequestEntity,
                                     @NonNull final Result<SetupTOTPMFAResponseEntity> result)
    {
        try{

            if (baseurl != null && !baseurl.equals("") && AccessToken != null && !AccessToken.equals("")) {
                //Todo Service call
                OauthService.getShared(context).setupTOTPMFA(baseurl, AccessToken,codeChallenge,setupTOTPMFARequestEntity,
                        new Result<SetupTOTPMFAResponseEntity>() {
                            @Override
                            public void success(final SetupTOTPMFAResponseEntity serviceresult) {

                                new CountDownTimer(5000, 500) {
                                    String instceID="";
                                    public void onTick(long millisUntilFinished) {
                                        instceID=getInstanceId();
                                        if(instceID!=null && instceID!="")
                                        {
                                            onFinish();
                                        }

                                    }

                                    public void onFinish() {
                                        if(instceID!=null && instceID!="")
                                        {
                                            //Todo Call Next Service cALL TO Validate DEVICE
                                            validateDevice(instceID, serviceresult.getData().getStatusId(), new Result<ValidateDeviceResponseEntity>() {
                                                @Override
                                                public void success(ValidateDeviceResponseEntity result) {
                                                    //Todo call Next service
                                                    scannedTOTP(result.getData().getUsage_pass(), serviceresult.getData().getStatusId(), AccessToken,new Result<ScannedResponseEntity>() {
                                                        @Override
                                                        public void success(ScannedResponseEntity result) {
                                                            Timber.i(result.getData().getUserDeviceId()+"USewr Device id");
                                                            Toast.makeText(context, result.getData().getUserDeviceId()+"USewr Device id", Toast.LENGTH_SHORT).show();
                                                        }

                                                        @Override
                                                        public void failure(WebAuthError error) {
                                                            Toast.makeText(context, "Error on Scanned"+error.getErrorMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                }

                                                @Override
                                                public void failure(WebAuthError error) {
                                                    Toast.makeText(context, "Error on validate Device"+error.getErrorMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                        else {
                                            // return Error Message
                                            webAuthError=WebAuthError.getShared(context).deviceVerificationFailureException();
                                            result.failure(webAuthError);
                                        }
                                    }
                                }.start();


                            }

                            @Override
                            public void failure(WebAuthError error) {
                                result.failure(error);
                            }
                        });
            }
            else
            {
                webAuthError=webAuthError.propertyMissingException();
                webAuthError.ErrorMessage="one of the Login properties missing";
                result.failure(webAuthError);
            }
        }
        catch (Exception e)
        {
            Timber.e(e.getMessage());
        }
    }

    //Scanned TOTP
    private void scannedTOTP(@NonNull String usagePass,@NonNull String statusId,@NonNull String AccessToken, @NonNull final Result<ScannedResponseEntity> result)
    {
        try {
            String baseurl="";
            if(savedProperties==null){

                savedProperties=DBHelper.getShared().getLoginProperties();
            }
            if(savedProperties==null){
                //Read from file if localDB is null
                readFromFile(new Result<Dictionary<String, String>>() {
                    @Override
                    public void success(Dictionary<String, String> loginProperties) {
                        savedProperties=loginProperties;
                    }

                    @Override
                    public void failure(WebAuthError error) {
                        result.failure(error);
                    }
                });
            }



            if (savedProperties.get("DomainURL").equals("") || savedProperties.get("DomainURL") == null || savedProperties == null) {
                webAuthError = webAuthError.propertyMissingException();
                String loggerMessage = "Setup TOTP MFA readProperties failure : " + "Error Code - " + webAuthError.errorCode + ", Error Message - " + webAuthError.ErrorMessage
                        + ", Status Code - " + webAuthError.statusCode;
                LogFile.addRecordToLog(loggerMessage);
                result.failure(webAuthError);
            }
            if (savedProperties.get("ClientId").equals("") || savedProperties.get("ClientId") == null || savedProperties == null) {
                webAuthError = webAuthError.propertyMissingException();
                String loggerMessage = "Accept Consent readProperties failure : " + "Error Code - " + webAuthError.errorCode + ", Error Message - " + webAuthError.ErrorMessage
                        + ", Status Code - " + webAuthError.statusCode;
                LogFile.addRecordToLog(loggerMessage);
                result.failure(webAuthError);
            }
            else {
                baseurl = savedProperties.get("DomainURL");

                if ( statusId != null && !statusId.equals("") && usagePass != null && !usagePass.equals("") && baseurl != null
                        && !baseurl.equals("")) {

                    scannedTOTPService(usagePass, baseurl,statusId,AccessToken,result);


                }

            }

        }
        catch (Exception e)
        {
            LogFile.addRecordToLog("acceptConsent exception"+e.getMessage());
            Timber.e("acceptConsent exception"+e.getMessage());
        }
    }

    //Service call To Scanned TOTP Service
    private void scannedTOTPService(@NonNull String usagePass,@NonNull String baseurl,
                                    @NonNull String statusId,@NonNull String AccessToken,
                                    @NonNull final Result<ScannedResponseEntity> scannedResponseResult)
    {
        try{

            if ( statusId != null && !statusId.equals("") && usagePass != null && !usagePass.equals("") && baseurl != null
                    && !baseurl.equals("")) {
                //Todo Service call

                if(codeChallenge==null){
                    generateChallenge();
                }
                OauthService.getShared(context).scannedTOTP(baseurl, usagePass, statusId,AccessToken,
                        new Result<ScannedResponseEntity>() {
                            @Override
                            public void success(final ScannedResponseEntity serviceresult) {
                                //Todo Call Scanned Service


                                scannedResponseResult.success(serviceresult);
                            }

                            @Override
                            public void failure(WebAuthError error) {
                                scannedResponseResult.failure(error);
                            }
                        });
            }
            else
            {
                webAuthError=webAuthError.propertyMissingException();
                webAuthError.ErrorMessage="one of the Login properties missing";
                scannedResponseResult.failure(webAuthError);
            }
        }
        catch (Exception e)
        {
            Timber.e(e.getMessage());
        }
    }

    //enrollTOTPMFA
    public void enrollTOTPMFA(@NonNull final EnrollTOTPMFARequestEntity enrollTOTPMFARequestEntity, @NonNull final Result<EnrollTOTPMFAResponseEntity> result){
        try {
            String baseurl="";
            if(savedProperties==null){

                savedProperties=DBHelper.getShared().getLoginProperties();
            }
            if(savedProperties==null){
                //Read from file if localDB is null
                readFromFile(new Result<Dictionary<String, String>>() {
                    @Override
                    public void success(Dictionary<String, String> loginProperties) {
                        savedProperties=loginProperties;
                    }

                    @Override
                    public void failure(WebAuthError error) {
                        result.failure(error);
                    }
                });
            }

            if (savedProperties.get("DomainURL").equals("") || savedProperties.get("DomainURL") == null || savedProperties == null) {
                webAuthError = webAuthError.propertyMissingException();
                String loggerMessage = "Setup TOTP MFA readProperties failure : " + "Error Code - " + webAuthError.errorCode + ", Error Message - " + webAuthError.ErrorMessage
                        + ", Status Code - " + webAuthError.statusCode;
                LogFile.addRecordToLog(loggerMessage);
                result.failure(webAuthError);
            } else {
                baseurl = savedProperties.get("DomainURL");

                if ( enrollTOTPMFARequestEntity.getVerifierPassword() != null && !enrollTOTPMFARequestEntity.getVerifierPassword().equals("") &&
                        enrollTOTPMFARequestEntity.getSub() != null && enrollTOTPMFARequestEntity.getSub()  != null &&
                        enrollTOTPMFARequestEntity.getStatusId() != null && enrollTOTPMFARequestEntity.getStatusId()  != null &&
                        baseurl != null && !baseurl.equals("")) {

                    final String finalBaseurl = baseurl;
                    getAccessToken(enrollTOTPMFARequestEntity.getSub(), new Result<AccessTokenEntity>() {
                        @Override
                        public void success(AccessTokenEntity accesstokenresult) {
                            enrollTOTPMFAService(accesstokenresult.getAccess_token(), finalBaseurl,enrollTOTPMFARequestEntity,result);
                        }

                        @Override
                        public void failure(WebAuthError error) {
                            result.failure(error);
                        }
                    });


                }
                else {
                    webAuthError=webAuthError.propertyMissingException();
                    webAuthError.ErrorMessage="one of the Login properties missing";
                    result.failure(webAuthError);
                }

            }

        }
        catch (Exception e)
        {
            LogFile.addRecordToLog("acceptConsent exception"+e.getMessage());
            Timber.e("acceptConsent exception"+e.getMessage());
        }
    }

    //Service call To enrollTOTPMFA
    private void enrollTOTPMFAService(@NonNull String AccessToken, @NonNull String baseurl,
                                      @NonNull final EnrollTOTPMFARequestEntity enrollTOTPMFARequestEntity, @NonNull final Result<EnrollTOTPMFAResponseEntity> result){
        try{

            if (enrollTOTPMFARequestEntity.getVerifierPassword() != null && !enrollTOTPMFARequestEntity.getVerifierPassword().equals("") &&
                    enrollTOTPMFARequestEntity.getSub() != null && enrollTOTPMFARequestEntity.getSub()  != null &&
                    enrollTOTPMFARequestEntity.getStatusId() != null && enrollTOTPMFARequestEntity.getStatusId()  != null &&
                    baseurl != null && !baseurl.equals("") && AccessToken != null && !AccessToken.equals("")) {
                //Todo Service call
                OauthService.getShared(context).enrollTOTPMFA(baseurl, AccessToken, enrollTOTPMFARequestEntity,new Result<EnrollTOTPMFAResponseEntity>() {
                    @Override
                    public void success(EnrollTOTPMFAResponseEntity serviceresult) {
                        result.success(serviceresult);
                    }

                    @Override
                    public void failure(WebAuthError error) {
                        result.failure(error);
                    }
                });
            }
            else
            {
                webAuthError=webAuthError.propertyMissingException();
                webAuthError.ErrorMessage="one of the Login properties missing";
                result.failure(webAuthError);
            }
        }
        catch (Exception e)
        {
            Timber.e(e.getMessage());
        }
    }

*/}
