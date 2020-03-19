package ru.ok.technopolis.animations;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SwipeListenerImpl implements ItemSwipeManger.SwipeListener {

    private final MoviesAdapter moviesAdapter;

    public SwipeListenerImpl(@NonNull MoviesAdapter moviesAdapter) {
        this.moviesAdapter = moviesAdapter;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            moviesAdapter.removeItem(position);
        }
    }

}
