package fragments;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import activities.Constants;
import adapters.JSONArrayAdapter;
import models.Contractor;
import models.ModelBuilder;
import munoz.pablo.directorio.AuthHelper;
import munoz.pablo.directorio.R;
import services.APIRequest;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ContractorDetail#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ContractorDetail extends Fragment implements
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    private TextView nameTv;
    private TextView idTv;
    private TextView phoneTv;
    private TextView emailTv;
    private ImageView portraitIv;
    private RatingBar overallRatingBar;
    private RatingBar myRatingBar;
    private ListView commentsLv;
    private EditText commentEt;
    private Button addCommentBtn;
    private MapView mMapView;

    private JSONArrayAdapter commentsAdapter;

    private Contractor contractor;
    private ModelBuilder<Contractor> modelBuilder;

    // the fragment initialization parameters
    private static final String ARG_contractorId = "contractorId";

    private String contractorId;

    private GoogleMap mMap;
    private GoogleApiClient client;
    private Location lastLocation;
    private LocationRequest locationRequest;


    public ContractorDetail() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param contractorId Parameter 1.
     * @return A new instance of fragment ContractorDetail.
     */
    // TODO: Rename and change types and number of parameters
    public static ContractorDetail newInstance(String contractorId) {
        ContractorDetail fragment = new ContractorDetail();
        Bundle args = new Bundle();
        args.putString(ARG_contractorId, contractorId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            this.contractorId = getArguments().getString(ARG_contractorId);
        }


    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_contractor_detail, container, false);

        Log.d("ContractorDetail", "Fragment created with contractorId = " + this.contractorId);

        this.nameTv = (TextView) view.findViewById(R.id.contractor_detail_name);
        this.idTv = (TextView) view.findViewById(R.id.contractor_detail_id);
        this.emailTv = (TextView) view.findViewById(R.id.contractor_detail_email);
        this.phoneTv = (TextView) view.findViewById(R.id.contractor_detail_phone);
        this.portraitIv = (ImageView) view.findViewById(R.id.contractor_detail_img);
        this.overallRatingBar = (RatingBar) view.findViewById(R.id.contractor_detail_rating_bar);
        this.commentsLv = (ListView) view.findViewById(R.id.contractor_detail_lv);

        this.commentEt = (EditText) view.findViewById(R.id.contractor_detail_comment_edit);
        this.commentEt.setVisibility(View.INVISIBLE);

        this.addCommentBtn = (Button) view.findViewById(R.id.contractor_detail_add_comment_btn);

        this.addCommentBtn.setOnClickListener(new View.OnClickListener() {
            ContractorDetail fragment = ContractorDetail.this;

            @Override
            public void onClick(View v) {
                if (fragment.commentEt.getVisibility() == View.VISIBLE) {
                    ContractorDetail.this.publishComment();
                } else {
                    fragment.commentEt.setVisibility(View.VISIBLE);
                }
            }
        });

        this.myRatingBar = (RatingBar) view.findViewById(R.id.contractor_detail_my_rating_bar);
        this.myRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                if (!fromUser) return;

                String token = AuthHelper.getAuthToken(getActivity());

                APIRequest apiRequest = new APIRequest(new APIRequest.APIRequestCallback() {
                    @Override
                    public void onSuccess(JSONObject json, int code) {
                    }

                    @Override
                    public void onError(String errorMessage, int code) {
                    }
                });

                String url = Constants.API_URL + "/api/v1/contractor/" + contractorId + "/rate/" + rating;

                apiRequest.execute(
                        APIRequest.HTTP_POST,
                        url,
                        "{ \"Authorization\": \"Bearer " + token + "\" }",
                        "{}");
            }
        });

        this.modelBuilder = new ModelBuilder<>();

        this.pullContractorData(contractorId);

        MapsInitializer.initialize(this.getActivity());
        this.mMapView = (MapView) view.findViewById(R.id.contractor_detail_map);
        this.mMapView.onCreate(savedInstanceState);
        this.mMapView.getMapAsync(this);

        if (this.client == null) {

            this.client = new GoogleApiClient.Builder(this.getActivity())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        this.locationRequest = LocationRequest.create();
        this.locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        this.locationRequest.setInterval(1000 * 5);
        this.locationRequest.setFastestInterval(1000 * 3);


        return view;
    }

    private void publishComment() {
        final ContractorDetail fragment = ContractorDetail.this;
        final Activity activity = fragment.getActivity();

        APIRequest request = new APIRequest(new APIRequest.APIRequestCallback() {
            @Override
            public void onSuccess(JSONObject json, int code) {
                fragment.pullContractorData(fragment.contractorId);
                fragment.commentEt.setVisibility(View.INVISIBLE);
                fragment.commentEt.setText("");
            }

            @Override
            public void onError(String errorMessage, int code) {
                Toast.makeText(activity, "No se pudo guardar tu comentario.", Toast.LENGTH_SHORT).show();
            }
        });

        String token = AuthHelper.getAuthToken(getActivity());

        JSONObject payload = new JSONObject();
        JSONObject headers = new JSONObject();
        try {
            headers.put("Authorization", "Bearer " + token);
            payload.put("content", fragment.commentEt.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        request.execute(APIRequest.HTTP_POST,
                Constants.API_URL + "/api/v1/contractor/" + fragment.contractorId + "/comment",
                headers.toString(),
                payload.toString());
    }

    private void pullContractorData(String contractorId) {
        APIRequest apiRequest = new APIRequest(new APIRequest.APIRequestCallback() {
            @Override
            public void onSuccess(JSONObject json, int code) {
                ContractorDetail self = ContractorDetail.this;

                try {
                    self.contractor =  self.modelBuilder.resourceFromJson(json);
                    self.updateView();
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void onError(String errorMessage, int code) {
                Log.e("ContractorDetail", "Error pulling contractor data from the API");
            }
        });

        apiRequest.execute(APIRequest.HTTP_GET, Constants.API_URL + "/api/v1/contractor/" + contractorId);
    }

    public void updateView() {
        this.nameTv.setText(this.contractor.getFullName());
        this.idTv.setText("" + this.contractor.getId());
        this.emailTv.setText(this.contractor.getEmail());
        this.phoneTv.setText(this.contractor.getPhone());
        this.overallRatingBar.setRating((float) this.contractor.getRating());
        this.myRatingBar.setRating(4);

        Glide.with(ContractorDetail.this)
                .load(this.contractor.getPortrait())
                .fitCenter()
                .into(this.portraitIv);

        this.commentsAdapter = new JSONArrayAdapter(getActivity(), this.contractor.getComments(),
                new JSONArrayAdapter.ViewBuilder() {
                    @Override
                    public View construct(JSONArray data, int position, View view, ViewGroup parent) {
                        if (view == null) {
                            view = getActivity().getLayoutInflater().inflate(R.layout.contractor_detail_comment, parent, false);
                        }

                        TextView contentTv = (TextView) view.findViewById(R.id.contractor_detail_comment_content);
                        try {
                            contentTv.setText(data.getJSONObject(position).getString("content"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        return view;
                    }
                });

        this.commentsLv.setAdapter(this.commentsAdapter);


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mMap = googleMap;


        Geocoder coder = new Geocoder(this.getActivity());

        List<Address> address = null;
        try {
            address = coder.getFromLocationName("Puebla 111, Guadalajara, Jalisco", 5);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (address != null) {
            LatLng elSalon = new LatLng(address.get(0).getLatitude(), address.get(0).getLongitude());
            mMap.addMarker(new MarkerOptions().position(elSalon).title(contractor.getFullName()));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(elSalon, 18));
        }


        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Intent navigation = new Intent(Intent.ACTION_VIEW, Uri
                        .parse("geo:0,0?q=" + Uri.encode("Calle Puebla #111, Guadalajara Jalisco")));
                startActivity(navigation);
            }
        });

    }

    public void setMyLocation() {

        if (ContextCompat.checkSelfPermission(this.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this.getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        } else {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            setMyLocation();
    }


    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public void onStart() {
        super.onStart();
        client.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        client.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        lastLocation = LocationServices.FusedLocationApi.getLastLocation(client);
        if (lastLocation != null) {

            Log.d("LAST LOCATION",
                    lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
        }

        if (ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
        Log.d("LOCATION CHANGED",
                lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}