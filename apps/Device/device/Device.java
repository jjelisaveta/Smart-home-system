package device;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import sun.net.www.http.HttpClient;
import java.util.Base64;
import java.util.Date;

/**
 *
 * @author Rica
 */

public class Device {
    private static String username;
    private static String password;
    private static Scanner s = new Scanner(System.in);
    private static String httpRequest = "http://localhost:8080/Service/resources/";
   
    public static void main(String[] args) {
        
        System.out.println("**********WELCOME**********");
        System.out.print("username:");
        username = s.nextLine();
       // System.out.print("password:");
        password = "nopassword";
        int opt;
        do {
            System.out.println("Menu:");
            System.out.println("   1. alarm");
            System.out.println("   2. planner");
            System.out.println("   3. music");
            System.out.println("   0. exit");
            System.out.print("Choose an option:");
            opt = s.nextInt();

            switch (opt){
                case 1:
                    alarmMenu();
                    break;
                case 2:
                    plannerMenu();
                    break;
                case 3:
                    musicMenu();
                    break;
            }
        } while (opt!=0);
        
    }
    
    private static void createRequest(String request, List<String> p) {
        try {
            String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
            request = httpRequest + request;
            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(request);
            httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + auth);
            
            if (p!=null){
                int size = p.size()/2;
                List<NameValuePair> params = new ArrayList<NameValuePair>(size);
                for(int i = 0; i<p.size();i+=2){
                    params.add(new BasicNameValuePair(p.get(i), p.get(i+1)));
                }
                httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            }
            HttpResponse response = httpclient.execute(httpPost);
            BufferedReader r = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = null;
            while ((line = r.readLine()) != null) 
               System.out.println(line);
            r.close();
        }catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Device.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Device.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void alarmMenu(){
        int opt = -1;
        do {
            System.out.println("Alarm menu:");
            System.out.println("   1. set custom alarm");
            System.out.println("   2. set custom periodic alarm");
            System.out.println("   3. choose alarm");
            System.out.println("   4. set song");
            System.out.println("   0. back");
            System.out.print("Choose an option:");
            opt = s.nextInt();
            List<String> params = new ArrayList<>();
            switch(opt){
                case 1: case 2:
                    System.out.print("  -date and time (dd.MM.yyyy. HH:mm): ");
                    s.nextLine();
                    String dateTime = s.nextLine();
                    int period = 0;
                    if (opt==2){
                        System.out.print("  -period: ");
                        period = s.nextInt();
                    }
                    params = createAlarmParams(dateTime, period);
                    createRequest("alarm/set", params);
                    break;
                case 3:
                    int opt3 = -1;
                    System.out.println("Set an alarm in:");
                    System.out.println("   1. 5 minutes");
                    System.out.println("   2. 30 minutes");
                    System.out.println("   3. 1 hour");
                    Date currentTime = new Date();
                    Date alarmTime = null;
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy. HH:mm");
                    opt3 = s.nextInt();
                    switch (opt3){
                        case 1:
                            alarmTime = Date.from(currentTime.toInstant().plusSeconds(300));
                            break;
                        case 2:
                            alarmTime = Date.from(currentTime.toInstant().plusSeconds(1800));
                            break;
                        case 3:
                            alarmTime = Date.from(currentTime.toInstant().plusSeconds(3600));
                            break;
                        default:
                            System.out.println("Error: Option does not exist.");
                            break;
                    }
                    params = createAlarmParams(sdf.format(alarmTime),0);
                    createRequest("alarm/set", params);
                    break;
                case 4:
                    System.out.print("Name of song: ");
                    s.nextLine();
                    String songName = s.nextLine();
                    System.out.println(songName);
                    params.add("song");
                    params.add(songName);
                    createRequest("alarm/song", params);
                    break;
            }
        } while(opt!=0);
    }
    
    private static void plannerMenu(){
        int opt = -1;
        do {
            System.out.println("Planner menu:");
            System.out.println("   1. show all tasks");
            System.out.println("   2. create task");
            System.out.println("   3. remove task");
            System.out.println("   4. edit task");
            System.out.println("   5. calculate travel time");
            System.out.println("   6. calculate travel time (from current location)");
            System.out.println("   0. break");
            System.out.print("Choose planner option:");
            opt = s.nextInt();
            
            List<String> params = new ArrayList<>();
            switch (opt){
                case 1:
                    createRequest("planner/get", null);
                    break;
                case 2:
                    System.out.println("Insert:");
                    System.out.print("  -time (dd.MM.yyyy. HH:mm):");
                    s.nextLine();
                    String dateTime = s.nextLine();
                    params.add("startTime");
                    params.add(dateTime);
                    System.out.print("  -duration:");
                    int duration = s.nextInt();
                    params.add("duration");
                    params.add(((Integer)duration).toString());
                    System.out.print("  -location (enter for no location):");
                    s.nextLine();
                    String loc = s.nextLine();
                    params.add("location");
                    params.add(loc);
                    System.out.print("  -set an alarm? (yes/no):");
                    String rem = s.nextLine();
                    params.add("reminder");
                    params.add(rem);
                    createRequest("planner/create", params);
                    break;
                case 3:
                    System.out.println("Insert:");
                    System.out.print("  -time (dd.MM.yyyy. HH:mm):");
                    s.nextLine();
                    String startTime = s.nextLine();
                    //System.out.println(startTime);
                    params.add("startTime");
                    params.add(startTime);
                    createRequest("planner/remove", params);
                    break;
                case 4:
                    System.out.print("IdTask you want to edit: ");
                    int id = s.nextInt();
                    params.add("id");
                    params.add(((Integer)id).toString());
                    System.out.print("  -new time (enter for same):");
                    s.nextLine();
                    String newTime = s.nextLine();
                    params.add("startTime");
                    params.add(newTime);
                    System.out.print("  -new duration (0 for same):");
                    int newDuration = s.nextInt();
                    params.add("duration");
                    if (newDuration == 0)
                        newDuration = -1;
                    params.add(((Integer)newDuration).toString());
                    System.out.print("  -new location (enter for same):");
                    s.nextLine();
                    String newLoc = s.nextLine();
                    params.add("location");
                    params.add(newLoc);
                    createRequest("planner/edit", params);
                    break;
                case 5:
                    System.out.println("Insert:");
                    System.out.print("  -location A:");
                    s.nextLine();
                    String locA = s.nextLine();
                    params.add("locationA");
                    params.add(locA);
                    System.out.print("  -location B:");
                    String locB = s.nextLine();
                    //System.out.println(locB);
                    params.add("locationB");
                    params.add(locB);
                    createRequest("planner/timeAB", params);
                    break;
                case 6:
                    System.out.println("Insert:");
                    System.out.print("  -destination:");
                    s.nextLine();
                    String dst = s.nextLine();
                    params.add("locationA");
                    params.add("");
                    params.add("locationB");
                    params.add(dst);
                    createRequest("planner/timeAB", params);
                    break;
                case 0:
                    break;
                default:
                    System.out.println("Error: Option does not exist.");
                    break;
            }
        } while (opt != 0);
    }
    
    private static void musicMenu(){
        int opt = -1;
        do {
            System.out.println("Music menu:");
            System.out.println("   1. play song");
            System.out.println("   2. history");
            System.out.println("   0. back");
            System.out.print("Choose music option:");
            opt = s.nextInt();
            switch(opt){
                case 1:
                    List<String> params = new ArrayList<>();
                    System.out.print("  -song:");
                    s.nextLine();
                    String songName = s.nextLine();
                    //System.out.println(songName);
                    params.add("song");
                    params.add(songName);
                    createRequest("speaker/play", params);
                    break;
                case 2:
                    createRequest("speaker/list", null);
                    break;
            }
        } while(opt!=0);
    }
    
    private static List<String> createAlarmParams(String dateTime, int period){
        List<String> params = new ArrayList<>();
        params.add("dateTime");
        params.add(dateTime);
        System.out.println(dateTime);
        params.add("period");
        params.add(((Integer)period).toString());
        return params;
    }
}