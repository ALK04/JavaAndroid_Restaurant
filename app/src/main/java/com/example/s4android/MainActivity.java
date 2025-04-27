package com.example.s4android;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.widget.Button;
import android.util.Log;
import java.sql.Connection;
import java.util.List;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    public Database database;
    int secteur = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // DEMANDER LA PERMISSION SI BESOIN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Token pour Firebase
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    Log.d("FCM", "Token: " + token);
                });


        int secteur = getIntent().getIntExtra("SECTEUR", -1);

        Button btnSecteur1 = findViewById(R.id.btnSecteur1);
        Button btnSecteur2 = findViewById(R.id.btnSecteur2);
        Button btnSecteur3 = findViewById(R.id.btnSecteur3);
        Button btnSecteur4 = findViewById(R.id.btnSecteur4);


        btnSecteur1.setOnClickListener(view -> ouvrirTables(1));
        btnSecteur2.setOnClickListener(view -> ouvrirTables(2));
        btnSecteur3.setOnClickListener(view -> ouvrirTables(3));
        btnSecteur4.setOnClickListener(view -> ouvrirTables(4));

//        database = new Database();
//        database.connectDB();

        Database db = new Database();
        Connection conn = db.connectDB();

        if (conn != null) {
            List<Statut_table> statuts = db.getStatuts(conn, secteur);
            for (Statut_table s : statuts) {
                Log.d("Statut", s.toString());
            }
        }
    }
    private void ouvrirTables(int secteur) {
        Intent intent = new Intent(this, GestionTable.class);
        intent.putExtra("SECTEUR", secteur);
        startActivity(intent);
    }

}

