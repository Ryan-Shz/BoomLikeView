package com.sc.github.view;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private BoomLikeView boomLikeView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boomLikeView = findViewById(R.id.boom_like_view);
        boomLikeView.setSrc(R.drawable.like);
        boomLikeView.setLikedImageResource(R.drawable.liked);

        final Button button = findViewById(R.id.set_like_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boomLikeView.setText("10");
                boomLikeView.setLike(true);
            }
        });

        boomLikeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (boomLikeView.isLike()) {
                    boomLikeView.unLike();
                } else {
                    boomLikeView.like();
                }
            }
        });

    }
}
