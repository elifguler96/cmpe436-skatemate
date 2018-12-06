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
import com.google.gson.Gson;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    Context context = this;
    FloatingActionButton messageButton;

    List<Spot> spots = new ArrayList<>();
    String clientUsername;

    CheckSpotUpdatesThread checkSpotUpdates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageButton = findViewById(R.id.btn_message);

        clientUsername = getIntent().getStringExtra("clientUsername");

        GetSpotsThread getSpotsThread = new GetSpotsThread();
        getSpotsThread.start();

        checkSpotUpdates = new CheckSpotUpdatesThread();
        checkSpotUpdates.start();

        messageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, ConversationActivity.class);
                intent.putExtra("clientUsername", clientUsername);
                startActivity(intent);
                try {
                    finishAndCloseConnections();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void finishAndCloseConnections() throws IOException {
        if (checkSpotUpdates != null) {
            checkSpotUpdates.closeConnection();
        }

        finish();
    }

    public void showMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (spots.size() > 0) {
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
        }

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

                        CreateSpotThread thread = new CreateSpotThread(new Spot(name, latLng.latitude, latLng.longitude));
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
                socket = new Socket("0.tcp.ngrok.io", 10252);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                Request request = new Request();
                request.clientUsername = clientUsername;
                request.type = RequestType.GETSPOTS;
                sendMessage(new Gson().toJson(request));

                String data;
                while ((data = in.readLine()) != null) {
                    Response response = new Gson().fromJson(data, Response.class);
                    if (response.code == 5) {
                        spots = response.spots;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showMap();
                            }
                        });
                        break;
                    }
                }

                closeConnection();
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

        void closeConnection() throws IOException {
            socket.close();
            in.close();
            out.close();
        }
    }

    class CheckSpotUpdatesThread extends Thread {
        Socket socket;
        BufferedReader in;
        BufferedWriter out;

        @Override
        public void run() {
            try {
                socket = new Socket("0.tcp.ngrok.io", 10252);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                String data;
                while (in.ready() && (data = in.readLine()) != null) {
                    Response response = new Gson().fromJson(data, Response.class);
                    if (response.code == 11) {
                        GetSpotsThread thread = new GetSpotsThread();
                        thread.start();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void closeConnection() throws IOException {
            socket.close();
            in.close();
            out.close();
        }
    }

    class CreateSpotThread extends Thread {
        Socket socket;
        BufferedReader in;
        BufferedWriter out;

        Spot spot;

        CreateSpotThread(Spot spot) {
            this.spot = spot;
        }

        @Override
        public void run() {
            try {
                socket = new Socket("0.tcp.ngrok.io", 10252);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                Request request = new Request();
                request.clientUsername = clientUsername;
                request.type = RequestType.CREATESPOT;
                request.spot = spot;

                sendMessage(new Gson().toJson(request));

                closeConnection();
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

        void closeConnection() throws IOException {
            socket.close();
            in.close();
            out.close();
        }
    }
}
