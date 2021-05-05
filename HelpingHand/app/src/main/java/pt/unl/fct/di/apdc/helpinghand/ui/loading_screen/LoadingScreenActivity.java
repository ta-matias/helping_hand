package pt.unl.fct.di.apdc.helpinghand.ui.loading_screen;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import pt.unl.fct.di.apdc.helpinghand.R;

public class LoadingScreenActivity extends AppCompatActivity {

    /**
     * Permissions Constants
     */
    private final String[] PERMISSIONS = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
    private final int REQUEST_COARSE_LOCATION = 0;
    private final int REQUEST_FINE_LOCATION = 1;

    public void OnCreate( Bundle savedInstance) {
        //Checks if it exists permissions to use the device location, and storage
        verifyPermissions();

        //Inflates layout
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_loading_screen);

    }

    private void verifyPermissions() {
        //Verifies Location permission
        verifyLocationPermission();

    }

    private void verifyLocationPermission(){
        int aCLPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int aFLPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        //Asks for permission to acess location
        if( aCLPermission != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, this.PERMISSIONS, this.REQUEST_COARSE_LOCATION);
        if( aFLPermission != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, this.PERMISSIONS, this.REQUEST_FINE_LOCATION);
    }
}
