package es.uniovi.amigos;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    public String FriendsURL = "http://6f09ca40.ngrok.io"+ "/api/Amigo";
    public String PUTFriendURL = "http://6f09ca40.ngrok.io"+ "/api/Amigo/";
    public Hashtable<Integer, Amigo> Amigo = new Hashtable<Integer, Amigo>();

    private String mUserName = null;
    private final static Long UPDATE_PERIOD = (long) 1000;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Timer timer = new Timer();
        TimerTask updateAmigos = new UpdateAmigoPosition();
        timer.scheduleAtFixedRate(updateAmigos, 0, UPDATE_PERIOD);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            askUserName();
        }
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    class UpdateAmigoPosition extends TimerTask {
        public void run() {
            new FriendsLocalize().execute(FriendsURL);
        }
    }

    public class Amigo {

        private String _Name;
        private double _Lati;
        private double _Longi;

        public Amigo(String name, double lati, double longi) {
            _Name = name;
            _Lati = lati;
            _Longi = longi;
        }

        public String getName() {
            return _Name;
        }

        public double getLongi() {
            return _Longi;
        }

        public double getLati() {
            return _Lati;
        }
    }


    public class FriendsLocalize extends AsyncTask<String, Void, String> {

        @Override
        protected void onPostExecute(String result) {
            mMap.clear();
            for (Map.Entry<Integer, Amigo> entry : Amigo.entrySet()) {
                Amigo Am = entry.getValue();
                mMap.addMarker(new MarkerOptions().position(new LatLng(Am.getLati(),Am.getLongi())).title(Am.getName()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            }
        }

        @Override
        protected String doInBackground(String... urls) {
            getCurrencyRateUsdRate(urls[0]);
            return "OK";
        }

        private void getCurrencyRateUsdRate(String url)  {
            try {
                parseDataFromNetwork(readStream(openUrl(url)));
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }

        protected InputStream openUrl(String urlString) throws IOException {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            // Starts the query
            conn.connect();
            return conn.getInputStream();
        }

        // Método que lee el contenido del stream
        protected String readStream(InputStream urlStream) throws IOException {
            BufferedReader r = new BufferedReader(new InputStreamReader(urlStream));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            return total.toString();
        }

        private void parseDataFromNetwork(String data) throws IOException, JSONException {

            Amigo.clear();
            JSONArray amigos = new JSONArray(data);
            for(int i = 0; i < amigos.length(); i++) {
                JSONObject amigoObject = amigos.getJSONObject(i);
                String Id = amigoObject.getString("ID");
                String Name = amigoObject.getString("name");
                String[] Longi = amigoObject.getString("longi").split(",");
                String[] Lati = amigoObject.getString("lati").split(",");
                double longiNumber;
                double latiNumber;
                try {
                    if(Longi.length == 1){
                        longiNumber = Double.parseDouble(Longi[0]);
                    }
                    else{
                        longiNumber = Double.parseDouble(Longi[0]+"."+Longi[1]);
                    }
                    if(Lati.length == 1){
                        latiNumber = Double.parseDouble(Lati[0]);
                    }
                    else {
                        latiNumber = Double.parseDouble(Lati[0]+"."+Lati[1]);
                    }
                    Amigo Am = new Amigo(Name,latiNumber,longiNumber);
                    int ID = Integer.parseInt(Id);
                    Amigo.put(ID,Am);
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                }

            }
        }
    }

    public void askUserName() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Settings");
        alert.setMessage("User name:");

        // Crear un EditText para obtener el nombre
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mUserName = input.getText().toString();
                SetupLocation();
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // llamar a una función para crear las peticiones de localización

                    //while (mUserName == null || Mutex == false){}
                    SetupLocation();

                } else {}
                return;
            }
        }
    }
    // Se define un Listener para escuchar por cambios en la posición
    class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            // Se llama cuando hay una nueva posición para ese location provider
            double lati = location.getLatitude();
            double longi = location.getLongitude();
            SetupLocation();
        }

        // Se llama cuando cambia el estado
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        // Se llama cuando se activa el provider
        @Override
        public void onProviderEnabled(String provider) {}

        // Se llama cuando se desactiva el provider
        @Override
        public void onProviderDisabled(String provider) {}
    }
    void SetupLocation()
    {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }


        // Se debe adquirir una referencia al Location Manager del sistema
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Se obtiene el mejor provider de posición
        Criteria criteria = new Criteria();
        String  provider = locationManager.getBestProvider(criteria, false);

        // Se crea un listener de la clase que se va a definir luego
        MyLocationListener locationListener = new MyLocationListener();

        // Se registra el listener con el Location Manager para recibir actualizaciones
        locationManager.requestLocationUpdates(provider, 0, 10, locationListener);

        // Comprobar si se puede obtener la posición ahora mismo
        Location location = locationManager.getLastKnownLocation(provider);

        //mUserName = "Amigo Lugoa";
        if (location != null && mUserName != null) {
            int id = CheckAmigo();
            if(id != -1){
                new SendAmigoToServer().execute(
                        PUTFriendURL,  String.valueOf(id),mUserName,
                        String.valueOf(location.getLatitude()),
                        String.valueOf(location.getLongitude()));
                // La posición actual es location
            }
            else{
                Toast.makeText(getApplicationContext(), "No existe ningún amigo con ese nombre",
                        Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(getApplicationContext(), "Amigo o localización nulos", Toast.LENGTH_SHORT).show();
        }
    }

    public int CheckAmigo(){
        int id = -1;
        for (Map.Entry<Integer, Amigo> entry : Amigo.entrySet()) {
            Amigo Am = entry.getValue();
            String _Amigo = Am.getName();
            if(mUserName.equals(_Amigo)){
                return id = entry.getKey();
            }
        }
        return id;
    }

    public class SendAmigoToServer extends AsyncTask<String, Void, String> {

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getApplicationContext(), "Cambio realizado con éxito",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(String... urls) {
            try {
                PutAmigo(urls[0], urls[1], urls[2], urls[3], urls[4]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "OK";
        }

        public void PutAmigo(String uurl, String id, String name, String lati,String longi) throws IOException {
            // Cambiamos los puntos por commas, sino da problema a la hora de convertir. ????????????????????????????????
            DataOutputStream dataOutputStream = null;
            HttpURLConnection httpPut = null;

            try {
                //String[] Lati = lati.split(".");
                //String[] Longi = lati.split(".");
                URL url = new URL(uurl + id);
                httpPut = (HttpURLConnection) url.openConnection();
                httpPut.setReadTimeout(10000 /* milliseconds */);
                httpPut.setConnectTimeout(15000 /* milliseconds */);
                httpPut.setRequestProperty("Content-Type", "application/json");
                httpPut.setDoOutput(true);
                httpPut.setDoInput(true);
                httpPut.setRequestMethod("PUT");
                // dataOutputStream = new DataOutputStream(httpPut.getOutputStream());
                // dataOutputStream.write(Integer.parseInt(id));
                // dataOutputStream.write(Integer.parseInt(lati));
                // dataOutputStream.write(Integer.parseInt(longi));
                OutputStreamWriter out = new OutputStreamWriter(httpPut.getOutputStream());
                //String payload = "{\"ID\":" + id + " ,\"name\":\""+name+"\",\"longi\":\""+Longi[0]+","+Longi[1]+"\",\"lati\":\""+Lati[0]+","+Lati[1]+"\"}";
                String payload = "{\"ID\":" + id + " ,\"name\":\""+name+"\",\"longi\":\""+longi+"\",\"lati\":\""+lati+"\"}";
                out.write(payload);
                out.close();
                httpPut.getInputStream();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    public class SetParams extends AsyncTask<String, Void, String> {

        @Override
        protected void onPostExecute(String result) {

        }

        @Override
        protected String doInBackground(String... urls) {
            return "OK";
        }

    }
}
