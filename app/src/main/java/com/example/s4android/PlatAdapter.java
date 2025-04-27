package com.example.s4android;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.widget.Button;


public class PlatAdapter extends RecyclerView.Adapter<PlatAdapter.MenuViewHolder> {

    private List<PlatItem> platItems;
    private OnPlatClickListener listener;
    private boolean modeResume;

    public PlatAdapter(List<PlatItem> platItems, OnPlatClickListener listener, boolean modeResume) {
        this.platItems = platItems;
        this.listener = listener;
        this.modeResume = modeResume;
    }

    public void setIsResume(boolean isResume) {
        this.modeResume = isResume;
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (modeResume) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_resume, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu, parent, false);
        }
        return new MenuViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        PlatItem item = platItems.get(position);

        if (modeResume) {
            // Mode résumé : affichage simple avec nom et prix
            holder.nomPlat.setText(item.getNom());
            holder.prixPlat.setText(String.format("€ %.2f", item.getPrix()));

            // Affichage des informations supplémentaires dans le résumé
            holder.textSauce.setText(item.getSauce() != null ? item.getSauce() : "-");
            holder.textCuisson.setText(item.getCuisson() != null ? item.getCuisson() : "-");
            holder.textCommentaire.setText(item.getCommentaire());

            // Dans le mode résumé, les boutons sont visibles uniquement si un compteur est > 0
            boolean afficherBoutons = item.getCompteur() > 0;
            holder.boutonSupprimer.setVisibility(afficherBoutons ? View.VISIBLE : View.GONE);
            holder.boutonCommentaire.setVisibility(afficherBoutons ? View.VISIBLE : View.GONE);
        } else {
            // Mode détail : affichage classique avec nom, prix, et compteur
            holder.nomPlat.setText(item.getNom());
            holder.prixPlat.setText(String.format("€ %.2f", item.getPrix()));
            holder.compteur.setText("x" + item.getCompteur());

            // Affichage des boutons de suppression et commentaire
            boolean afficherBoutons = item.getCompteur() > 0;
            holder.boutonSupprimer.setVisibility(afficherBoutons ? View.VISIBLE : View.GONE);
            holder.boutonCommentaire.setVisibility(afficherBoutons ? View.VISIBLE : View.GONE);
            holder.compteur.setVisibility(afficherBoutons ? View.VISIBLE : View.GONE);
        }

        // Gestion des clics
        holder.itemView.setOnClickListener(v -> {
            if (!modeResume) {
                listener.onPlatClick(item); // Logic for item click in detailed mode
            }
        });

        holder.boutonSupprimer.setOnClickListener(v -> {
            listener.onPlatSupprime(item); // Logic for item delete in detailed mode
        });

        holder.boutonCommentaire.setOnClickListener(v -> {
            listener.onCommentaireClick(item); // Logic for item comment in detailed mode
        });
    }


    @Override
    public int getItemCount() {
        return platItems.size();
    }

    public interface OnPlatClickListener {
        void onPlatClick(PlatItem item);
        void onPlatSupprime(PlatItem item);
        void onCommentaireClick(PlatItem item);
    }

    public static class MenuViewHolder extends RecyclerView.ViewHolder {
        TextView nomPlat, prixPlat, compteur, textCuisson, textSauce, textCommentaire;
        Button boutonSupprimer, boutonCommentaire;

        public MenuViewHolder(View itemView) {
            super(itemView);
            nomPlat = itemView.findViewById(R.id.nom);
            prixPlat = itemView.findViewById(R.id.prix);
            compteur = itemView.findViewById(R.id.compteur);
            boutonSupprimer = itemView.findViewById(R.id.boutonSupprimer);
            boutonCommentaire = itemView.findViewById(R.id.boutonCommentaire);

            // Initialisation des TextViews pour les infos supplémentaires en mode résumé
            textCuisson = itemView.findViewById(R.id.textCuisson);
            textSauce = itemView.findViewById(R.id.textSauce);
            textCommentaire = itemView.findViewById(R.id.textCommentaire);
        }
    }

}