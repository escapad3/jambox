package com.escapad3.jambox;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.specification.*;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import com.wrapper.spotify.requests.data.library.GetUsersSavedTracksRequest;
import com.wrapper.spotify.requests.data.playlists.GetListOfCurrentUsersPlaylistsRequest;
import com.wrapper.spotify.requests.data.users_profile.GetCurrentUsersProfileRequest;
import com.wrapper.spotify.requests.data.search.simplified.SearchTracksRequest;

import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.Date;

@RestController
public class SpotifyConnect {
//    String clientAppCallbackUrl = "http://localhost:8090/callback/";
//    String clientAppCallbackUrl = "http://localhost:3000/callback/";
    String clientAppCallbackUrl = "https://jamplayer.herokuapp.com/callback/";

    private static final String clientId = System.getenv("SPOTIFY_CLIENT_ID");
    private static final String clientSecret = System.getenv("SPOTIFY_CLIENT_SECRET");
    private static final String spotifyRedirectUri = System.getenv("SPOTIFY_REDIRECT_URI");
    private static final URI redirectUri = SpotifyHttpManager.makeUri(spotifyRedirectUri);

    private static final SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setRedirectUri(redirectUri)
            .build();
    //          .state("x4xkmn9pu3j6ukrs8n")
    private static final AuthorizationCodeUriRequest authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
            .scope("user-read-birthdate,user-read-email,user-library-read")
//            .show_dialog(true)
            .build();

    private AuthorizationCodeRequest authorizationCodeRequest;
    private AuthorizationCodeCredentials authorizationCodeCredentials;

    Date currentDate = new Date();

    long timestampTimer = currentDate.getTime();

    @CrossOrigin(origins = "*")
    @RequestMapping("/")
    public void jamboxHome (HttpServletResponse response) throws IOException {
        String uri = "/spotifyconnect/";
        response.sendRedirect(uri);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping("/spotifyconnect/")
//    public void authorizationCodeUri_Sync(HttpServletResponse response) throws IOException {
    public String authorizationCodeUri_Sync(HttpServletResponse response) throws IOException {
//        clientAppCallbackUrl = clientReturnUrl;
        URI uri = authorizationCodeUriRequest.execute();
        return uri.toString();
//        response.sendRedirect(uri.toString());
    }

    @CrossOrigin(origins = "*")
    @RequestMapping("/callback/")
    public void authorizationCode_Sync(@RequestParam("code") String code, HttpServletResponse response) {
        authorizationCodeRequest = spotifyApi.authorizationCode(code).build();
        try {
            authorizationCodeCredentials = authorizationCodeRequest.execute();

            // Set access and refresh token for further "spotifyApi" object usage
            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
            spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());

            timestampTimer = currentDate.getTime() + (1000 * authorizationCodeCredentials.getExpiresIn());
            response.sendRedirect(clientAppCallbackUrl);
//            response.sendRedirect("/profile/");
//            return "Expires in: " + authorizationCodeCredentials.getExpiresIn();
        } catch (IOException | SpotifyWebApiException e) {
//            return "Error: " + e.getMessage();
        }

    }

    private static final AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = spotifyApi.authorizationCodeRefresh().build();

    void authorizationCodeRefresh_Sync() {
        try {
            final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRefreshRequest.execute();

            // Set access and refresh token for further "spotifyApi" object usage
            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
            spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());

            timestampTimer = currentDate.getTime() + (1000 * authorizationCodeCredentials.getExpiresIn());
//            System.out.println("Expires in: " + authorizationCodeCredentials.getExpiresIn());
        } catch (IOException | SpotifyWebApiException e) {
//            System.out.println("Error: " + e.getMessage());
        }
    }

    boolean tokenTimedOut(){
        boolean b = currentDate.getTime() >= timestampTimer;
        return b;
    }

    void refreshToken(){
        if (tokenTimedOut()) {
            authorizationCodeRefresh_Sync();
        }
    }

    @CrossOrigin(origins = "*")
    @RequestMapping("/profile/")
    public User getCurrentUsersProfile_Sync() {
        refreshToken();
        try {
            final GetCurrentUsersProfileRequest getCurrentUsersProfileRequest = spotifyApi.getCurrentUsersProfile().build();

            final User user = getCurrentUsersProfileRequest.execute();

            return user;
        } catch (IOException | SpotifyWebApiException e) {
//            return "Error: " + e.getMessage();
            return null;
        }
    }

    @CrossOrigin(origins = "*")
    @RequestMapping("/search")
    public Track[] searchSongs(@RequestParam("searchBox") String q){
        final SearchTracksRequest searchTracksRequest = spotifyApi.searchTracks(q).build();
        try {
            final Paging<Track> trackPaging = searchTracksRequest.execute();
            return trackPaging.getItems();
        } catch (IOException | SpotifyWebApiException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return new Track[0];
    }

    @CrossOrigin(origins = "*")
    @RequestMapping("/playlists")
    public PlaylistSimplified[] userPlaylists(){
        final GetListOfCurrentUsersPlaylistsRequest getListOfCurrentUsersPlaylistsRequest = spotifyApi.getListOfCurrentUsersPlaylists().build();
        try {
            final Paging<PlaylistSimplified> playlistSimplifiedPaging = getListOfCurrentUsersPlaylistsRequest.execute();

            return playlistSimplifiedPaging.getItems();
        } catch (IOException | SpotifyWebApiException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return new PlaylistSimplified[0];
    }

    @CrossOrigin(origins = "*")
    @RequestMapping("/collection")
    public Track[] userCollection(){
        final GetUsersSavedTracksRequest getUsersSavedTracksRequest = spotifyApi.getUsersSavedTracks().build();
        try {
            final Paging<SavedTrack> savedTrackPaging = getUsersSavedTracksRequest.execute();
            int total = savedTrackPaging.getLimit();
            Track[] savedSongs = new Track[total];
            SavedTrack[] songsList = savedTrackPaging.getItems();
            for (int i = 0; i < total; i++) {
                savedSongs[i] = songsList[i].getTrack();
            }
            return savedSongs;
        } catch (IOException | SpotifyWebApiException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return new Track[0];
    }
}
