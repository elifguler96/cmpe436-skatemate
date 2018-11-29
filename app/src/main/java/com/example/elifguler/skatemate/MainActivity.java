package com.example.elifguler.skatemate;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    Context context = this;
    FloatingActionButton messageButton;

    List<Spot> spots = new ArrayList<>();
    ArrayList<Conversation> conversations = new ArrayList<>();

    String clientUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageButton = findViewById(R.id.btn_message);

        clientUsername = getIntent().getStringExtra("clientUsername");

        GetSpotsThread getSpotsThread = new GetSpotsThread();
        getSpotsThread.start();

        CheckSpotUpdatesThread checkSpotUpdates = new CheckSpotUpdatesThread();
        checkSpotUpdates.start();

        messageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GetConversationsThread thread = new GetConversationsThread();
                thread.start();
            }
        });
    }

    public void showMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Add a marker for each spot,
        // and move the map's camera to the same location.
        double maxLat = Integer.MIN_VALUE;
        double maxLng = Integer.MIN_VALUE;
        double minLat = Integer.MAX_VALUE;
        double minLng = Integer.MAX_VALUE;
        for (Spot spot : spots) {
            LatLng latLng = new LatLng(spot.lat, spot.lng);
            googleMap.addMarker(new MarkerOptions().position(latLng).title(spot.name));
            maxLat = Math.max(maxLat, latLng.latitude);
            maxLng = Math.max(maxLng, latLng.longitude);
            minLat = Math.min(minLat, latLng.latitude);
            minLng = Math.min(minLng, latLng.longitude);
        }

        LatLngBounds bounds = new LatLngBounds(new LatLng(minLat, minLng), new LatLng(maxLat, maxLng));
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 50);
        googleMap.animateCamera(cameraUpdate);

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(final LatLng latLng) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Add New Spot");

                // Set up the input
                final EditText input = new EditText(context);
                input.setHint("Enter name of spot");
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString();
                        spots.add(new Spot(name, latLng.latitude, latLng.longitude));
                        showMap();

                        CreateSpotThread thread = new CreateSpotThread(name, "" + latLng.latitude, "" + latLng.longitude);
                        thread.start();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });
    }

    class GetSpotsThread extends Thread {
        Socket socket;
        BufferedReader in;
        BufferedWriter out;

        @Override
        public void run() {
            try {
                socket = new Socket("ec2-35-180-63-125.eu-west-3.compute.amazonaws.com", 2909);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                sendMessage(clientUsername + "$GETSPOTS$");

                String data;
                while ((data = in.readLine()) != null) {
                    if (data.charAt(0) == '5') {
                        data = data.substring(2);

                        String[] spotStrings = data.split("-");
                        for (String spot : spotStrings) {
                            String name = spot.substring(0, spot.indexOf("$"));
                            double lat = Double.parseDouble(spot.substring(spot.indexOf("$") + 1, spot.lastIndexOf("$")));
                            double lng = Double.parseDouble(spot.substring(spot.lastIndexOf("$") + 1));
                            spots.add(new Spot(name, lat, lng));
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showMap();
                            }
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void sendMessage(String message) {
            try {
                out.write(message);
                out.newLine();
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class CheckSpotUpdatesThread extends Thread {
        Socket socket;
        BufferedReader in;
        BufferedWriter out;

        @Override
        public void run() {
            try {
                socket = new Socket("ec2-35-180-63-125.eu-west-3.compute.amazonaws.com", 2909);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                String data;
                while (true) {
                    data = in.readLine();
                    if (data != null && data.equals("11$")) {
                        GetSpotsThread thread = new GetSpotsThread();
                        thread.start();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class CreateSpotThread extends Thread {
        Socket socket;
        BufferedReader in;
        BufferedWriter out;

        String name;
        String lat;
        String lng;

        CreateSpotThread(String name, String lat, String lng) {
            this.name = name;
            this.lat = lat;
            this.lng = lng;
        }

        @Override
        public void run() {
            try {
                socket = new Socket("ec2-35-180-63-125.eu-west-3.compute.amazonaws.com", 2909);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                sendMessage(clientUsername + "$CREATESPOT$" + name + "$" + lat + "$" + lng);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void sendMessage(String message) {
            try {
                out.write(message);
                out.newLine();
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class GetConversationsThread extends Thread {
        Socket socket;
        BufferedReader in;
        BufferedWriter out;

        @Override
        public void run() {
            try {
                socket = new Socket("ec2-35-180-63-125.eu-west-3.compute.amazonaws.com", 2909);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                sendMessage(clientUsername + "$GETCONVERSATIONS$");

                String data;
                while ((data = in.readLine()) != null && data.length() > 0) {
                    if (data.charAt(0) == '3') {
                        data = data.substring(2);

                        String[] conversationStrings = data.split("-");
                        for (String conversation : conversationStrings) {
                            String[] parts = conversation.split("\\.txt\\$");
                            String[] usernames = parts[0].split("_");

                            String messagesString = parts[1];
                            ArrayList<String> messagesList = new ArrayList<>(Arrays.asList(messagesString.split("#")));
                            conversations.add(new Conversation(usernames[0], usernames[1], messagesList));
                        }
                    }


                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(context, ConversationActivity.class);
                            intent.putParcelableArrayListExtra("conversations", conversations);
                            intent.putExtra("clientUsername", clientUsername);
                            startActivity(intent);
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void sendMessage(String message) {
            try {
                out.write(message);
                out.newLine();
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
