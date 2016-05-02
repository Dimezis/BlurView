package com.eightbitlab.blurview_sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import butterknife.BindView;

public class ListFragment extends BaseFragment {
    @BindView(R.id.recyclerView) RecyclerView recyclerView;

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
        recyclerView.setAdapter(new ExampleListAdapter(getContext()));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }
}
