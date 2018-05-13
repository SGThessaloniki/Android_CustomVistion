package com.example.user.customvision;

import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Button camera_button;
    ImageView imageView;
    TextView result_tv;
    Bitmap bitmap;
    File myFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera_button=(Button)findViewById(R.id.camera_button_id);
        imageView=(ImageView)findViewById(R.id.image_id);
        result_tv=(TextView)findViewById(R.id.resultTV_id);


        //The activation of the camera when "open camera"  button is pressed.
        camera_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent,0);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        bitmap=(Bitmap)data.getExtras().get("data");
        //Compress of the image in jpeg form.
        try{
            bitmap.compress(Bitmap.CompressFormat.JPEG,90,new FileOutputStream(
                    myFile=new File(getFilesDir(),"temp.jpg")
            ));
            imageView.setImageBitmap(bitmap);
            callAPI();
        }catch (FileNotFoundException e){
            //this line is used in every catch. It is a great way to find the error.
            e.printStackTrace();
        }
    }


    /**
     * This function calls the API that app uses to connect with the custom vision service.
     * In order to use some of the classes at this function it is necessary to add a dependency and a librabry
     * at build.gradle(Module:app) and a permission at Manifest.xml
     * (Go to these files and you will find instructions).
     */
    public void callAPI(){
        //A thread is necessary for the networking
        new Thread(new Runnable() {
            @Override
            public void run() {
                /*this URL is unique for every model and it is created when the model is trained
                using https://www.customvision.ai/
                */
                String myUrl="https://southcentralus.api.cognitive.microsoft.com/customvision/v1.1/Prediction/1dcf3fca-1dd8-4d89-b11c-cd3d5646d93b/image?";
                HttpClient httpClient= HttpClients.createDefault();
                //At this point the API will be called. There is a request and a response.
                try{
                    URIBuilder builder=new URIBuilder(myUrl);
                    URI uri=builder.build();

                    HttpPost request=new HttpPost(uri);

                    /*These elements are unique for every model and they are created when the model is trained
                    using https://www.customvision.ai/ */
                    request.setHeader("Content-Type","application/json");
                    request.setHeader("Prediction-Key","d97653c9e1b54a809b692767aa2e5ecf");


                    //Convertion of the image file to byte array in order to use it for the request.
                    int size=(int)myFile.length();
                    byte[] bytes=new byte[size];
                    try{
                        BufferedInputStream buf=new BufferedInputStream(
                                new FileInputStream(myFile)
                        );
                        buf.read(bytes,0,bytes.length);
                        buf.close();
                    }catch (FileNotFoundException e){
                        e.printStackTrace();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                    request.setEntity(new ByteArrayEntity(bytes));

                    HttpResponse response=httpClient.execute(request);
                    HttpEntity entity=response.getEntity();


                    //If request and response completed correctly we will take a json file with the results back.
                    if(entity!=null){
                        //Gain the json to use the information that it has.
                        String jsonString= EntityUtils.toString(entity);
                        JSONObject json=new JSONObject(jsonString);
                        //Just a check on console.
                        System.out.println("REST Response:\n");
                        System.out.println(json.toString(2));

                        //index variable used to find the max element in order to print it on the user interface.
                        JSONArray pred=json.getJSONArray("Predictions");
                        ArrayList<Double> prob=new ArrayList<>();
                        int index=0;
                        prob.add(pred.getJSONObject(index).getDouble("Probability"));
                        for(int i=1;i<pred.length();i++){
                            int j=i;
                            prob.add(pred.getJSONObject(i).getDouble("Probability"));
                            if(prob.get(j)>prob.get(j--)){
                                index=i;
                            }
                        }

                        /*Print of the results on the user interface.
                         * To achieve this a new thread is necessary because at this point we are in the
                         * thread that is used for networking. */
                        final String tag=pred.getJSONObject(index).getString("Tag");
                        final Double probab=pred.getJSONObject(index).optDouble("Probability");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //If probability is too low it means that it is not a flower.
                                if(probab<0.02){
                                    result_tv.setText("It is not a flower!");
                                }
                                //Otherwise print on the user interface the element with the highest probability.
                                else{
                                    result_tv.setText("It is a " + tag + " with probability " + probab);
                                }
                            }
                        });

                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }
}

