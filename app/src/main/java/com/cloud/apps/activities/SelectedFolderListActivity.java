package com.cloud.apps.activities;

import static com.cloud.apps.utils.Consts.MY_PREFS_NAME;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cloud.apps.adapters.FullFolderAdapter;
import com.cloud.apps.databinding.ActivitySelectedFolderListBinding;
import com.cloud.apps.dialogs.LoadingDialog;
import com.cloud.apps.dialogs.SelectFolderDialog;
import com.cloud.apps.driveApi.GoogleDriveServiceHelper;
import com.cloud.apps.utils.Functions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import net.steamcrafted.loadtoast.LoadToast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SelectedFolderListActivity extends AppCompatActivity implements SelectFolderDialog.SelectListener {

    private String TAG = "SelectedFolderListActivity";
    ActivitySelectedFolderListBinding binding;

    ArrayList<String> folders;
    FullFolderAdapter adapter;

    GoogleDriveServiceHelper driverServiceHelper;

    public static LoadingDialog loadingDialog;

    LoadToast loadToast;
    SelectFolderDialog selectFolderDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySelectedFolderListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().setTitle("Selected Folder Being Synced");

        selectFolderDialog = new SelectFolderDialog(this, this, 0);
        folders = new ArrayList<>();
        loadingDialog = new LoadingDialog();
        loadToast = new LoadToast(this);


        if (GoogleSignIn.getLastSignedInAccount(this) != null) {
            GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(
                            this, Collections.singleton(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccount(GoogleSignIn.getLastSignedInAccount(this).getAccount());
            Drive googleDriveService =
                    new Drive.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            new GsonFactory(),
                            credential)
                            .setApplicationName("Drive API Migration")
                            .build();
            driverServiceHelper = new GoogleDriveServiceHelper(this, googleDriveService);
        }

        binding.addFab.setOnClickListener(v -> {
            selectFolderDialog.show(getSupportFragmentManager(), selectFolderDialog.getTag());
        });


        adapter = new FullFolderAdapter(this, folders, driverServiceHelper);
        binding.selectedFoldersRv.setLayoutManager(new LinearLayoutManager(this));
        binding.selectedFoldersRv.setAdapter(adapter);
        loadRecyclerView();
    }

    public void loadRecyclerView() {
        folders.clear();
        folders.addAll(Functions.getPaths(getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE), "selected_paths"));
        if (folders.size() > 0)
            binding.noSelectedFolderTv.setVisibility(View.GONE);
        else {
            binding.noSelectedFolderTv.setVisibility(View.VISIBLE);
        }
        Collections.sort(folders, (o1, o2) -> o1.toLowerCase().compareTo(o2.toLowerCase()));
        adapter.notifyItemRangeChanged(0, folders.size());
    }


    @Override
    public void onSelect(ArrayList<String> paths, int type) {
        Set<String> pathSet = new HashSet<>();
        pathSet.addAll(Functions.getPaths(getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE), "selected_paths"));
        pathSet.addAll(paths);
        Functions.setPaths("selected_paths", pathSet, getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit());
        loadRecyclerView();
    }
}
