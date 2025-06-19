package com.eightbitlab.blurview_sample;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ListFragment extends BaseFragment {

    @Override
    int getLayoutId() {
        return R.layout.fragment_list;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
    }

    private void init() {
        RecyclerView recyclerView = getView().findViewById(R.id.recyclerView);
        recyclerView.setAdapter(new ExampleListAdapter(getContext()));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        int initialPadding = recyclerView.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            recyclerView.setPadding(0, insets.top, 0, insets.bottom + initialPadding);
            return windowInsets;
        });
    }
}
