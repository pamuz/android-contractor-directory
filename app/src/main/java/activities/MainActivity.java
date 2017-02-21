package activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import adapters.ContractorCategoryAdapter;
import models.ContractorCategory;
import models.ModelBuilder;
import munoz.pablo.directorio.R;
import services.RESTCallback;
import services.RESTService;

public class MainActivity extends AppCompatActivity {

    private ContractorCategoryAdapter categoriesAdapter;
    private ArrayList<ContractorCategory> contractorCategoryList;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        contractorCategoryList = new ArrayList<>();

        this.categoriesAdapter = new ContractorCategoryAdapter(this, contractorCategoryList);

        listView = (ListView) findViewById(R.id.main_categories);
        listView.setAdapter(categoriesAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ContractorCategory category = contractorCategoryList.get(position);

                Intent intent = new Intent(MainActivity.this, ContractorsByCategory.class);
                intent.putExtra("categoryId", category.getId()) ;

                startActivity(intent);
            }
        });

        this.loadContractorCategoriesData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_register:
                this.goToContractorRegistrationActivity();
                return true;

            case R.id.action_settings:
                Toast.makeText(this, "TODO: go to settings activity.", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.action_refresh:
                MainActivity.this.loadContractorCategoriesData();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void goToContractorRegistrationActivity() {
        Intent intent = new Intent(this, ContractorRegistration.class);
        startActivity(intent);
    }

    private void loadContractorCategoriesData() {
        RESTService restApi = new RESTService();
        restApi.get(
                "http://192.168.33.10:3000/api/v1/contractor_category",
                new RESTCallback() {
                    @Override
                    public void onSuccess(JSONObject responseJson) {
                        try {
                            // HACK: ModelBuilder returns an Arraylist<Object>, the
                            // activity works with an ArrayList<ContractorCategory> but
                            // we cannot cast between them, we must first cast to ArrayList<?>.
                            contractorCategoryList = (ArrayList<ContractorCategory>)(ArrayList<?>)
                                    ModelBuilder.resourceListFromJson(
                                    ContractorCategory.class,
                                    responseJson
                            );
                            MainActivity.this.categoriesAdapter.addAll(contractorCategoryList);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(String rawResponse) {
                        Log.d("MainActivity", "Failed to retrieve contractor categories");
                    }
                });
    }
}
