package com.client;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.view.RedirectView;
import org.apache.commons.codec.binary.Base64;
import java.io.*;
import org.json.*;
import java.net.*;

@RestController
public class AppRestController {

    String id_token = null;
    String nickname = null;
    String picture = null;
    String msgVerify = "JWT Not Verified Yet";

    @RequestMapping(value = "/login-auth0", method=RequestMethod.GET)
    public RedirectView processForm1() {
        RedirectView redirectView = new RedirectView();

        //Prepare the OAuth Authorization URL
        String url = "https://thuva.auth0.com/authorize"+
                    "?audience=https://thuva.auth0.com/api/v2/"+
                    "&scope=openid%20profile"+
                    "&response_type=code"+
                    "&client_id=2QvmXCZQeP15UmGDlBrRrGFwpYVndKws"+
                    "&redirect_uri=http%3A%2f%2flocalhost%3A9100%2foauth%2faccess"+
                    "&state=123";
        redirectView.setUrl(url);
        return redirectView;
    }
    
    //Get the reponse recieved from auth0 after the user authorization
    @RequestMapping(value = "/oauth/access", method = RequestMethod.GET)
    public RedirectView authUser(ModelMap model, @RequestParam(value = "code",required=true) String authCode) {
        
        try{
            //Get the access token and id_token from the auth0 server by providing the code received
            String response = getAuthResponse(authCode);

            //Extract the user data from the received response
            JSONObject jsonBody = getUserData(response);
            this.nickname = jsonBody.getString("nickname");
            this.picture = jsonBody.getString("picture");

            //Set the attribute to the page and redirect the user to the user's home page
            return viewHomePage();
        }
        catch(Exception ex){
            System.out.println(ex);
        }
        return null;
    }

    @RequestMapping("/verify")
    public RedirectView verify() {
        AppRestVerifier app = new AppRestVerifier();
        if(app.validateJWTSignature(this.id_token))
            this.msgVerify = "JWT Verified";
        else
            this.msgVerify = "JWT Verification Failed";
        return viewHomePage();
    }

    public String getAuthResponse(String authCode)
    {
        ////Prepare the POST request to get the access_token and id_token 
        
        //OAuth Token URL
        String auth_url = "https://thuva.auth0.com/oauth/token";
        
        //Prepare POST Request Body
        String POST_PARAMS = "grant_type=authorization_code"+
                            "&client_id=2QvmXCZQeP15UmGDlBrRrGFwpYVndKws"+
                            "&client_secret=mNPDfC4J-sBUGaZZINZs2vTBNab-T3Co0MOWawpjufZ-zgoY2ZDar4OkCudsL_sF"+
                            "&code="+authCode+
                            "&redirect_uri=http%3A%2f%2flocalhost%3A9100%2foauth%2faccess";
        String authReponse = "";

        try
        {
            //Create the objects of URL and the HttpURLConnection and set the request method to POST
            URL obj = new URL(auth_url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");

            //Set Headers
            con.setRequestProperty("content-type", "application/x-www-form-urlencoded");

            //Set Body
            con.setDoOutput(true);
            OutputStream os = con.getOutputStream();
            os.write(POST_PARAMS.getBytes());
            os.flush();
            os.close();


            //Execute and get the response
            int responseCode = con.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK)//success
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null)
                {
                    response.append(inputLine);
                }
                in.close();

                authReponse = response.toString();

            }
            else
            {
                System.out.println("Error : " + responseCode);
            }
        }
        catch (Exception ex)
        {
            System.out.println(ex);
        }
        
        //Return the response received
        return authReponse;
    }

    public JSONObject getUserData(String response)
    {
        JSONObject bodyJson = null;
        try{
            //Convert the data to json object and retrive id_token
            JSONObject jsonObj = new JSONObject(response);
            this.id_token = jsonObj.getString("id_token");
            
            //return validateJWTSignature(id_token);
            //validateJWTSignature(id_token);

            //Spliit the id_token - JWT (Header, Body and Signature)
            String[] arr_spliit = id_token.split("\\.");
            String headEnc = arr_spliit[0];
            String bodyEnc = arr_spliit[1];
            String sigEnc = arr_spliit[2];

            //Decode the retrieved base64 encoded body
            Base64 base64Url = new Base64(true);
            String body = new String(base64Url.decode(bodyEnc));

            //Convert the body string to json object and retrieve user data
            bodyJson = new JSONObject(body);
        }
        catch(Exception ex){
            System.out.println(ex);
        }
        return bodyJson;
    }

    public RedirectView viewHomePage(){
        AppController app = new AppController();
        app.setModelAttribute("nickname",this.nickname);
        app.setModelAttribute("picture",this.picture);
        app.setModelAttribute("verify_status",this.msgVerify);
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl("/home");
        return redirectView;
    }
}