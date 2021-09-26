package speaker1;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.customsearch.v1.Customsearch;
import com.google.api.services.customsearch.v1.CustomsearchRequestInitializer;
import com.google.api.services.customsearch.v1.model.Result;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Rica
 * 
 */


public class MusicPlayer {
    private String songName;
    
    private static final String ENGINE_ID = "engine_id";
    private static final String API_KEY = "api_key";
    
    
    public MusicPlayer(String sn){
        songName = sn;
    }
    
    public void playSong(){
        try {
            Customsearch cs = new Customsearch.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), null)
                    .setApplicationName("Speaker1")
                    .setGoogleClientRequestInitializer(new CustomsearchRequestInitializer(API_KEY))
                    .build();
            
            //Set search parameter
            if (songName.equals(""))
                songName = "default alarm sound";
            Customsearch.Cse.List list = cs.cse().list().setQ(songName).setCx(ENGINE_ID);
            
            //Execute search
            List<Result> results = list.execute().getItems();
            
            // Open the browser
            if (results.size() >= 1) {
                Result result = results.get(0);
                URL url = new URL(result.getLink());
                Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
                if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                    try {
                        desktop.browse(url.toURI());
                    } catch (URISyntaxException ex) {
                        Logger.getLogger(Speaker.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(Speaker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } catch (GeneralSecurityException ex) {
            Logger.getLogger(Speaker.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Speaker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
