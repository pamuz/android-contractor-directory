package munoz.pablo.directorio.fragments;


import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import munoz.pablo.directorio.activities.MainActivity;
import munoz.pablo.directorio.adapters.ContractorCategoryAdapter;
import munoz.pablo.directorio.models.ContractorCategory;
import munoz.pablo.directorio.models.ModelBuilder;
import munoz.pablo.directorio.R;
import munoz.pablo.directorio.services.APIRequest;
import munoz.pablo.directorio.services.APIRequest2;
import munoz.pablo.directorio.utils.Constants;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ContractorCategoryMenu#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ContractorCategoryMenu extends Fragment {

    private ContractorCategoryAdapter categoriesAdapter;
    private ArrayList<ContractorCategory> contractorCategoryList;
    private ListView listView;
    private ProgressBar progressBar;
    private ModelBuilder<ContractorCategory> modelBuilder;

    public ContractorCategoryMenu() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ContractorCategoryMenu.
     */
    public static ContractorCategoryMenu newInstance() {
        ContractorCategoryMenu fragment = new ContractorCategoryMenu();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view =  inflater.inflate(R.layout.fragment_contractor_category_menu, container, false);

        progressBar = (ProgressBar) view.findViewById(R.id.category_list_loading);
        modelBuilder = new ModelBuilder<>();
        contractorCategoryList = new ArrayList<>();
        categoriesAdapter = new ContractorCategoryAdapter(view.getContext(), contractorCategoryList);
        listView = (ListView) view.findViewById(R.id.category_list_view);

        listView.setAdapter(categoriesAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MainActivity activity = (MainActivity) getActivity();
                Fragment categoryContractors = CategoryContractors.newInstance(contractorCategoryList.get(position).getId());
                activity.changeContentFragment(categoryContractors);
            }
        });

        requestContractorCategoriesDataFromApi();

        return view;
    }

    private void requestContractorCategoriesDataFromApi() {
        APIRequest2 req = new APIRequest2.Builder()
                .url(Constants.API_URL + "/api/v1/contractor_category")
                .method(APIRequest2.METHOD_GET)
                .callback(new APIRequest2.Callback() {
                    @Override
                    public void onResult(int responseCode, JSONObject response) {
                        if (responseCode != 200) return;

                        contractorCategoryList = modelBuilder.instantiateMany(response);

                        categoriesAdapter.clear();
                        categoriesAdapter.addAll(contractorCategoryList);
                        progressBar.setVisibility(View.GONE);
                    }
                })
                .build();

        req.execute();
    }
}