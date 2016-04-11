package tess.com.ttest;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;


import com.edmodo.rangebar.RangeBar;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    FFmpeg ffmpeg;
    Button but, res, con;
    private ProgressDialog progressDialog;
    VideoView mVideoView;
    long startT;
    int count;
    TextView cut;
    long temp_end, temp_start;
    private SeekBar sbStart, sbEnd;
    TextView st,endr;
    long endT;
    int max_prog = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        startT = 0;
        count = 0;

        // Gets the RangeBar
        st = (TextView)findViewById(R.id.starttime);
        endr = (TextView)findViewById(R.id.endtime);
        sbStart = (SeekBar)findViewById(R.id.seek);
        sbEnd = (SeekBar)findViewById(R.id.seek_end);

        cut = (TextView)findViewById(R.id.cut);
        cut.setText("");
        but = (Button)findViewById(R.id.button);
        res = (Button) findViewById(R.id.button2);
        res.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //resume the video
                mVideoView.seekTo((int) startT);
                mVideoView.start();
                sbEnd.postDelayed(onEverySecond, 60);
            }
        });
        con = (Button) findViewById(R.id.button3);
        con.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // join all the videos


                writeToFile();
                String rtsd = Environment.getExternalStorageDirectory().toString() + "/VideoTag";


                String cmd = "-f concat -i "+rtsd+"/input.txt -c copy "+rtsd+"/final_output.mp4";
                execFFmpegBinary(cmd);
                // delet all the residual temp files

//                String root_sd = Environment.getExternalStorageDirectory().toString();
//                Log.d("file path = ", root_sd);
//                File f = new File( root_sd+"/bluetooth") ;
//                final File[] files = f.listFiles( new FilenameFilter() {
//                    @Override
//                    public boolean accept( final File dir,
//                                           final String name ) {
//                        return name.matches( "output.*\\.mp4" );
//                    }
//                } );
//                for ( final File file : files ) {
//                    System.out.println("file name = "+ file.getName());
//                    if ( !file.delete() ) {
//                        System.err.println( "Can't remove " + file.getAbsolutePath() );
//                    }
//                }
//                // delete input.txt file
//                File fi = new File(root_sd+"/bluetooth/input.txt");
//                fi.delete();

            }
        });

        but.setOnClickListener(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        String root_sd = Environment.getExternalStorageDirectory().toString();
        File fnew = new File( root_sd+"/VideoTag") ;
        if(!fnew.exists()) {
            fnew.mkdirs();
            // add the video to that location
            InputStream in = getResources().openRawResource(R.raw.vid);
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(root_sd+"/VideoTag/vid.mp4");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            byte[] buff = new byte[1024];
            int read = 0;

            try {
                while ((read = in.read(buff)) > 0) {
                    out.write(buff, 0, read);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }


        Log.d("file path = ", root_sd);
        File f = new File( root_sd+"/VideoTag") ;
        final File[] files = f.listFiles( new FilenameFilter() {
            @Override
            public boolean accept( final File dir,
                                   final String name ) {
                return name.matches( "output.*\\.mp4" );
            }
        } );
        for ( final File file : files ) {
            System.out.println("file name = "+ file.getName());
            if ( !file.delete() ) {
                System.err.println( "Can't remove " + file.getAbsolutePath() );
            }
        }
        // delete input.txt file
        File fi = new File(root_sd+"/VideoTag/input.txt");
        fi.delete();
        File fi2 = new File(root_sd+"/VideoTag/final_output.mp4");
        fi2.delete();



        //video viewer
        mVideoView  = (VideoView) findViewById(R.id.videoView);


        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
        {
            @Override
            public void onPrepared(MediaPlayer arg0)
            {
                sbStart.setMax(mVideoView.getDuration());

                sbEnd.setMax(mVideoView.getDuration());
                sbEnd.postDelayed(onEverySecond, 60);
            }
        });

        sbStart.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {

                if(fromUser) {
                    // this is when actually seekbar has been seeked to a new position
                    startT = progress;
                    mVideoView.seekTo(progress);
                    //set the time
                    String hm = getTimeFormat(progress);
                    st.setText(hm);

                }
            }
        });

        sbEnd.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {

                if(fromUser) {
                    // this is when actually seekbar has been seeked to a new position
                   // startT = progress;
                   // mVideoView.seekTo(progress);
                    //set the time
                    max_prog = progress;
                    String hm = getTimeFormat(progress);
                    endr.setText(hm);

                }
            }
        });



        MediaController mc = new MediaController(this);
        mc.setAnchorView(mVideoView);
        mc.setMediaPlayer(mVideoView);

        mVideoView.setMediaController(mc);
        mVideoView.setVideoPath(root_sd+"/VideoTag/vid.mp4");
        mVideoView.start();
        ffmpeg = FFmpeg.getInstance(this);
        loadFFMpegBinary();

    }

    private Runnable onEverySecond=new Runnable() {

        @Override
        public void run() {

            if(sbEnd != null) {

                int g = mVideoView.getCurrentPosition();
                Log.d("play time = ", ""+g+", "+max_prog);
                if(g > max_prog) {
                    sbEnd.setProgress(g);
                    String hm = getTimeFormat(g);
                    endr.setText(hm);
                }
            }

            if(mVideoView.isPlaying()) {
                sbEnd.postDelayed(onEverySecond, 1000);
            }

        }
    };



    private void loadFFMpegBinary() {
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    // showUnsupportedExceptionDialog();
                }
            });
        } catch (FFmpegNotSupportedException e) {
            // showUnsupportedExceptionDialog();
        }
    }


    private void execFFmpegBinary(final String command) {
        try {

            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    //   addTextViewToLayout("FAILED with output : "+s);
                    Log.e("ffmp", "Error command : ffmpeg "+s);
                    progressDialog.dismiss();
                }

                @Override
                public void onSuccess(String s) {
                    //  addTextViewToLayout("SUCCESS with output : "+s);
                    Toast.makeText(getBaseContext(), "Done!", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();

                    // if video exists
                    String rtsd = Environment.getExternalStorageDirectory().toString() + "/VideoTag";
                    File fi2 = new File(rtsd+"/final_output.mp4");
                    if(fi2.exists()) {
                        Intent i = new Intent(MainActivity.this, PlayVid.class);
                        startActivity(i);
                        finish();
                    }
                    else {
                        String t = cut.getText().toString();
                        String clipS = getTimeFormat(temp_start);
                        String clipE = getTimeFormat(temp_end);
                        t += ("Clip "+count+" "+clipS + " to "+ clipE + "\n");
                        cut.setText(t);
                    }
                   }

                @Override
                public void onProgress(String s) {
                    Log.d("ffmp", "Started command : ffmpeg "+command);
                    // addTextViewToLayout("progress : "+s);
                    progressDialog.setMessage("Processing\n");
                }

                @Override
                public void onStart() {
                    //   outputLayout.removeAllViews();

                    Log.d("ffmp", "Started command : ffmpeg " + command);
                    progressDialog.setMessage("Processing...");
                    progressDialog.show();
                }


            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // do nothing for now
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        else if (id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        mVideoView.pause();
        endT = sbEnd.getProgress();

        long millis = endT - startT;

        temp_start = startT;
        temp_end = endT;

        String hms =   getTimeFormat(startT);
        String duration = getTimeFormat(millis);
      //  startT = endT;

        System.out.println("time ho rha h = "+hms);

        String root_sd = Environment.getExternalStorageDirectory().toString();

        count++;
        String cmd = "-i "+root_sd+"/VideoTag/vid.mp4 -ss "+hms+" -c copy -t "+duration+" "+root_sd+"/VideoTag/output"+count+".mp4";
        execFFmpegBinary(cmd);


    }
    public String getTimeFormat(long millis) {
        return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
        //return
    }

    private void writeToFile() {
        String rtsd = Environment.getExternalStorageDirectory().toString() + "/VideoTag";
        File dir = new File (rtsd);
      //  dir.mkdirs();
        File file = new File(dir, "input.txt");

        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);
            // write matter
            final File[] files = dir.listFiles( new FilenameFilter() {
                @Override
                public boolean accept( final File dir,
                                       final String name ) {
                    return name.matches( "output.*\\.mp4" );
                }
            } );
            //String catFile = "";
            if(files.length == 0) {
                Toast.makeText(getBaseContext(), "no files to concat", Toast.LENGTH_SHORT).show();
                return;
            }
            for ( final File fie : files ) {
                System.out.println("file name = "+ fie.getName());
                pw.println("file \'"+rtsd+"/"+fie.getName()+"\'");
          //      catFile += (root_sd+"\\"+file.getName())+"|";
            }
            //

            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i("write", "******* File not found. Did you" +
                    " add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
