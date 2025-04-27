package com.example.s4android;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Commande extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PlatAdapter platAdapter;
    private List<PlatItem> platItems;
    private List<PlatItem> platsCommandes;
    private TextView textViewResume;
    private Button buttonValiderCommande;
    private boolean isResumeMode = false;
    private int idTable = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.commande);

        recyclerView = findViewById(R.id.recyclerViewMenu);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        textViewResume = findViewById(R.id.textViewResume);
        textViewResume.setVisibility(View.GONE);

        buttonValiderCommande = findViewById(R.id.button_valider_commande);
        buttonValiderCommande.setVisibility(View.GONE);

        platItems = new ArrayList<>();
        platsCommandes = new ArrayList<>();

        idTable = getIntent().getIntExtra("ID_TABLE", -1);

        setupAdapter(false);

        findViewById(R.id.button13).setOnClickListener(v -> chargerMenu("Entrée"));
        findViewById(R.id.button14).setOnClickListener(v -> chargerMenu("Plat"));
        findViewById(R.id.button15).setOnClickListener(v -> chargerMenu("Dessert"));
        findViewById(R.id.button16).setOnClickListener(v -> chargerMenu("Boisson"));
        findViewById(R.id.button12).setOnClickListener(v -> afficherResume());

        buttonValiderCommande.setOnClickListener(v -> {
            if (platsCommandes.isEmpty()) {
                Toast.makeText(this, "Aucun plat à envoyer", Toast.LENGTH_SHORT).show();
                return;
            }

            Database db = new Database();
            Connection conn = db.connectDB();

            if (conn != null) {
                new Thread(() -> {
                    try {
                        Statement stmt = conn.createStatement();
                        int idCommande = -1;

                        // Vérifie si la table est occupée
                        String requete_table = "SELECT ST.STATUT_TABLE FROM TABLES T " +
                                "JOIN STATUT_TABLE ST ON T.ID_STATUT_TABLE = ST.ID_STATUT_TABLE " +
                                "WHERE T.ID_TABLES = " + idTable;
                        ResultSet rsEtat = stmt.executeQuery(requete_table);

                        boolean tableOccupee = false;
                        if (rsEtat.next()) {
                            String statut = rsEtat.getString("STATUT_TABLE");
                            tableOccupee = statut != null && statut.equalsIgnoreCase("occupée");
                        }

                        if (tableOccupee) {
                            ResultSet rsCommande = stmt.executeQuery(
                                    "SELECT ID_COMMANDE FROM COMMANDE WHERE ID_TABLES = " + idTable +
                                            " AND COMMANDE_PAYEE = FALSE ORDER BY DATE_COMMANDE DESC LIMIT 1");
                            if (rsCommande.next()) {
                                idCommande = rsCommande.getInt("ID_COMMANDE");
                            } else {
                                throw new Exception("Aucune commande non payée trouvée.");
                            }
                        } else {
                            stmt.executeUpdate(
                                    "INSERT INTO COMMANDE (DATE_COMMANDE, COMMANDE_PAYEE, ID_TABLES) " +
                                            "VALUES (NOW(), FALSE, " + idTable + ")", Statement.RETURN_GENERATED_KEYS);
                            ResultSet rsCmd = stmt.getGeneratedKeys();
                            if (rsCmd.next()) {
                                idCommande = rsCmd.getInt(1);
                            } else {
                                throw new Exception("Erreur création commande.");
                            }
                            stmt.executeUpdate("UPDATE TABLES SET ID_STATUT_TABLE = " +
                                    "(SELECT ID_STATUT_TABLE FROM STATUT_TABLE WHERE STATUT_TABLE = 'occupée') " +
                                    "WHERE ID_TABLES = " + idTable);
                        }

                        // ➡️ Séparer boissons et plats
                        List<PlatItem> boissons = new ArrayList<>();
                        List<PlatItem> autresPlats = new ArrayList<>();

                        for (PlatItem item : platsCommandes) {
                            if (isBoisson(item)) {
                                boissons.add(item);
                            } else {
                                autresPlats.add(item);
                            }
                        }

                        // ➡️ Créer un ticket pour les plats
                        if (!autresPlats.isEmpty()) {
                            int idTicketPlats = createTicket(stmt, idCommande);
                            for (PlatItem item : autresPlats) {
                                insertItem(stmt, item, idTicketPlats);
                            }
                        }

                        // ➡️ Créer un ticket pour les boissons
                        if (!boissons.isEmpty()) {
                            int idTicketBoissons = createTicket(stmt, idCommande);
                            for (PlatItem item : boissons) {
                                insertItem(stmt, item, idTicketBoissons);
                            }
                        }

                        runOnUiThread(() -> {
                            Toast.makeText(this, "Commande enregistrée avec séparation boissons/plats !", Toast.LENGTH_LONG).show();
                            platsCommandes.clear();
                            afficherResume();
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                }).start();
            } else {
                Toast.makeText(this, "Connexion impossible", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isBoisson(PlatItem item) {
        boolean result = false;
        Database db = new Database();
        Connection conn = db.connectDB();

        if (conn != null) {
            try {
                Statement stmt = conn.createStatement();
                String query = "SELECT TYPE_PLAT FROM PLAT " +
                        "JOIN TYPE_PLATS ON PLAT.ID_TYPE_PLATS = TYPE_PLATS.ID_TYPE_PLATS " +
                        "WHERE NOM_PLAT = '" + item.getNom().replace("'", "''") + "'";
                ResultSet rs = stmt.executeQuery(query);
                if (rs.next()) {
                    result = "Boisson".equalsIgnoreCase(rs.getString("TYPE_PLAT"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private int createTicket(Statement stmt, int idCommande) throws Exception {
        stmt.executeUpdate("INSERT INTO TICKET (TICKET_EN_COURS, ID_COMMANDE) VALUES (TRUE, " + idCommande + ")", Statement.RETURN_GENERATED_KEYS);
        ResultSet rsTicket = stmt.getGeneratedKeys();
        if (rsTicket.next()) {
            return rsTicket.getInt(1);
        } else {
            throw new Exception("Erreur lors de la création du ticket");
        }
    }

    private void insertItem(Statement stmt, PlatItem item, int idTicket) throws Exception {
        if (item.getCompteur() > 0) {
            String commentaire = item.getCommentaire().replace("'", "''");

            // Récupération ID_PLAT
            ResultSet rsPlat = stmt.executeQuery("SELECT ID_PLAT FROM PLAT WHERE NOM_PLAT = '" + item.getNom().replace("'", "''") + "'");
            int idPlat = rsPlat.next() ? rsPlat.getInt("ID_PLAT") : -1;

            // Récupération ID_SAUCE
            String sauce = item.getSauce();
            ResultSet rsSauce = stmt.executeQuery("SELECT ID_SAUCE FROM SAUCE WHERE NOM = '" + sauce.replace("'", "''") + "'");
            int idSauce = rsSauce.next() ? rsSauce.getInt("ID_SAUCE") : 1; // défaut 1 si pas trouvé

            // Récupération ID_CUISSON
            String cuisson = item.getCuisson();
            ResultSet rsCuisson = stmt.executeQuery("SELECT ID_CUISSON_VIANDE FROM CUISSON_VIANDE WHERE CUISSON = '" + cuisson.replace("'", "''") + "'");
            int idCuisson = rsCuisson.next() ? rsCuisson.getInt("ID_CUISSON_VIANDE") : 1; // défaut 1 si pas trouvé

            for (int i = 0; i < item.getCompteur(); i++) {
                stmt.executeUpdate("INSERT INTO ITEM (COMMENTAIRE, ID_SAUCE, ID_CUISSON_VIANDE, ID_PLAT, ID_TICKET) " +
                        "VALUES ('" + commentaire + "', " + idSauce + ", " + idCuisson + ", " + idPlat + ", " + idTicket + ")");
            }
        }
    }


    private void setupAdapter(boolean isResume) {
        this.isResumeMode = isResume;

        platAdapter = new PlatAdapter(platItems, new PlatAdapter.OnPlatClickListener() {
            @Override
            public void onPlatClick(PlatItem clickedItem) {
                clickedItem.incrementer();

                // Créer une copie indépendante du plat sélectionné
                PlatItem copyItem = new PlatItem(clickedItem.getNom(), clickedItem.getPrix());
                copyItem.setContientViande(clickedItem.getContientViande());
                copyItem.setContientSauce(clickedItem.getContientSauce());
                copyItem.setCompteur(1); // chaque ajout = 1 exemplaire
                copyItem.setCommentaire(""); // on laisse vide ou on ajoute plus tard

                platsCommandes.add(copyItem);

                if (clickedItem.getContientSauce()) {
                    showModaleSauce(copyItem, () -> {
                        if (copyItem.getContientViande()) {
                            showModaleViande(copyItem);
                        }
                    });
                } else if (copyItem.getContientViande()) {
                    showModaleViande(copyItem);
                }

                if (isResumeMode) {
                    afficherResume();
                } else {
                    platAdapter.notifyDataSetChanged();
                }

                Toast.makeText(Commande.this, clickedItem.getNom() + " ajouté", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPlatSupprime(PlatItem clickedItem) {
                clickedItem.decrementer();
                if (clickedItem.getCompteur() <= 0) {
                    platsCommandes.remove(clickedItem);
                }

                if (isResumeMode) {
                    afficherResume();
                } else {
                    platAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCommentaireClick(PlatItem item) {
                View dialogView = getLayoutInflater().inflate(R.layout.modale_commentaire, null);
                EditText editCommentaire = dialogView.findViewById(R.id.edit_commentaire);
                editCommentaire.setText(item.getCommentaire());

                AlertDialog dialog = new AlertDialog.Builder(Commande.this)
                        .setView(dialogView)
                        .create();

                Button btnOk = dialogView.findViewById(R.id.btn_ok);

                btnOk.setOnClickListener(v -> {
                    String commentaireSaisi = editCommentaire.getText().toString().trim();
                    String ancienCommentaire = item.getCommentaire();

                    // Fusionne l'ancien commentaire avec le nouveau
                    String nouveauCommentaire = (ancienCommentaire == null ? "" : ancienCommentaire);

                    if (!commentaireSaisi.isEmpty()) {
                        nouveauCommentaire += (nouveauCommentaire.isEmpty() ? "" : " | ") + commentaireSaisi;
                    }

                    item.setCommentaire(nouveauCommentaire);

                    Toast.makeText(Commande.this, "Commentaire enregistré", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });

                dialog.show();
            }


        }, isResume);

        recyclerView.setAdapter(platAdapter);
    }

    private void chargerMenu(String category) {
        textViewResume.setVisibility(View.GONE);
        Database db = new Database();
        Connection conn = db.connectDB();

        if (conn != null) {
            try {
                String query = "SELECT PLAT.ID_PLAT, PLAT.NOM_PLAT, PLAT.PRIX, PLAT.SUR_LA_CARTE, TYPE_PLATS.TYPE_PLAT, " +
                        "PLAT.CONTIENT_CUISSON, PLAT.CONTIENT_SAUCE " +
                        "FROM PLAT " +
                        "JOIN TYPE_PLATS ON PLAT.ID_TYPE_PLATS = TYPE_PLATS.ID_TYPE_PLATS " +
                        "WHERE TYPE_PLATS.TYPE_PLAT = '" + category + "' AND PLAT.SUR_LA_CARTE = TRUE";

                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query);

                platItems.clear();
                while (rs.next()) {
                    String nom = rs.getString("NOM_PLAT");
                    double prix = rs.getDouble("PRIX");
                    boolean contientViande = rs.getBoolean("CONTIENT_CUISSON");
                    boolean contientSauce = rs.getBoolean("CONTIENT_SAUCE");

                    PlatItem existingItem = null;
                    for (PlatItem cmdItem : platsCommandes) {
                        if (cmdItem.getNom().equals(nom)) {
                            existingItem = cmdItem;
                            break;
                        }
                    }

                    if (existingItem != null) {
                        platItems.add(existingItem);
                    } else {
                        PlatItem platItem = new PlatItem(nom, prix);
                        platItem.setContientViande(contientViande);
                        platItem.setContientSauce(contientSauce);
                        platItems.add(platItem);
                    }
                }

                runOnUiThread(() -> setupAdapter(false));
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Erreur récupération plats", Toast.LENGTH_SHORT).show());
            }
        } else {
            Toast.makeText(this, "Erreur de connexion", Toast.LENGTH_SHORT).show();
        }
    }

    private void afficherResume() {
        textViewResume.setVisibility(View.VISIBLE);
        platItems.clear();
        platItems.addAll(platsCommandes);
        buttonValiderCommande.setVisibility(platItems.size() > 0 ? View.VISIBLE : View.GONE);
        setupAdapter(true);
    }


    private void showModaleSauce(PlatItem item, Runnable onFinish) {
        View dialogView = getLayoutInflater().inflate(R.layout.modale_sauce_commentaire, null);
        AlertDialog dialog = new AlertDialog.Builder(Commande.this).setView(dialogView).create();

        Button[] buttons = {
                dialogView.findViewById(R.id.button),
                dialogView.findViewById(R.id.button2),
                dialogView.findViewById(R.id.button3),
                dialogView.findViewById(R.id.button4),
                dialogView.findViewById(R.id.button5),
        };

        List<String> sauces = getSaucesFromDB();
        for (int i = 0; i < buttons.length; i++) {
            if (i < sauces.size()) {
                final String sauce = sauces.get(i);
                buttons[i].setText(sauce);
                buttons[i].setVisibility(View.VISIBLE);
                buttons[i].setOnClickListener(v -> {
                    item.setSauce(sauce);
                    Toast.makeText(Commande.this, "Sauce sélectionnée : " + sauce, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    if (onFinish != null) onFinish.run();
                });
            } else {
                buttons[i].setVisibility(View.GONE);
            }
        }

        dialog.show();
    }

    private void showModaleViande(PlatItem item) {
        View dialogView = getLayoutInflater().inflate(R.layout.modale_viande_commentaire, null);
        AlertDialog dialog = new AlertDialog.Builder(Commande.this).setView(dialogView).create();

        Button[] buttons = {
                dialogView.findViewById(R.id.button),
                dialogView.findViewById(R.id.button2),
                dialogView.findViewById(R.id.button3),
                dialogView.findViewById(R.id.button4),
                dialogView.findViewById(R.id.button5),
        };

        List<String> cuissons = getCuissonsFromDB();
        for (int i = 0; i < buttons.length; i++) {
            if (i < cuissons.size()) {
                final String cuisson = cuissons.get(i);
                buttons[i].setText(cuisson);
                buttons[i].setVisibility(View.VISIBLE);
                buttons[i].setOnClickListener(v -> {
                    String current = item.getCommentaire();
                    item.setCuisson(cuisson);
                    Toast.makeText(Commande.this, "Cuisson sélectionnée : " + cuisson, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            } else {
                buttons[i].setVisibility(View.GONE);
            }
        }

        dialog.show();
    }

    private List<String> getSaucesFromDB() {
        List<String> sauces = new ArrayList<>();
        Database db = new Database();
        Connection conn = db.connectDB();

        if (conn != null) {
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT NOM FROM SAUCE");
                while (rs.next()) {
                    sauces.add(rs.getString("NOM"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sauces;
    }

    private List<String> getCuissonsFromDB() {
        List<String> cuissons = new ArrayList<>();
        Database db = new Database();
        Connection conn = db.connectDB();

        if (conn != null) {
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT CUISSON FROM CUISSON_VIANDE");
                while (rs.next()) {
                    cuissons.add(rs.getString("CUISSON"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return cuissons;
    }
}

