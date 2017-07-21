package neu.cse.coordinatordragmenu;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private CoordinatorDragMenu coordinatorDragMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        coordinatorDragMenu = (CoordinatorDragMenu) findViewById(R.id.coordinatorDragMenu);
        coordinatorDragMenu.setOnMainViewClickListener(new CoordinatorDragMenu.OnMainViewClickListener() {
            @Override
            public void onMainViewClick() {
                if (coordinatorDragMenu.isOpenedMenu()) {
                    coordinatorDragMenu.closeMenu();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (coordinatorDragMenu.isOpenedMenu()) {
            coordinatorDragMenu.closeMenu();
        } else {
            super.onBackPressed();
        }
    }
}
