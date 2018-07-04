package daslab.com.fieldmonitoringv2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;
import java.util.List;

import static daslab.com.fieldmonitoringv2.MapsActivity.plansDir;

public class activity_open_plan extends AppCompatActivity {

    private RecyclerView.Adapter adapter;

    public static interface ClickListener{
        public void onClick(View view,int position);
        public void onLongClick(View view,int position);
    }

    class RecyclerTouchListener implements RecyclerView.OnItemTouchListener{

        private ClickListener clicklistener;
        private GestureDetector gestureDetector;

        public RecyclerTouchListener( Context context, final RecyclerView recycleView, final ClickListener clicklistener){

            this.clicklistener=clicklistener;
            gestureDetector=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child=recycleView.findChildViewUnder(e.getX(),e.getY());
                    if(child!=null && clicklistener!=null){
                        clicklistener.onLongClick(child,recycleView.getChildAdapterPosition(child));
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            View child=rv.findChildViewUnder(e.getX(),e.getY());
            if(child!=null && clicklistener!=null && gestureDetector.onTouchEvent(e)){
                clicklistener.onClick(child,rv.getChildAdapterPosition(child));
            }

            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {

        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }


    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_open_plan);
        final List<String> plans = new LinkedList<>();
        Log.d("plans", plansDir.getAbsolutePath());
        File plansFile = new File(plansDir.getAbsolutePath().concat("/"));
        File[] plansFiles = plansFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                plans.add(f.getName());
                return f.isDirectory();
            }
        });
        Log.d("plansLength","Folders count: " + plansFiles.length);
        final RecyclerView recyclerView = findViewById(R.id.listView);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new PlanAdapter(plans,getApplicationContext());

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                recyclerView, new ClickListener(){
            @Override
            public void onClick(View view, final int position) {
                //Values are passing to activity & to fragment as well
                Toast.makeText(activity_open_plan.this, "Opening " + plans.get(position),
                        Toast.LENGTH_SHORT).show();
                Intent mapsActivityIntent = new Intent(activity_open_plan.this, MapsActivity.class);
                mapsActivityIntent.putExtra("planNameToOpen",plans.get(position));
                activity_open_plan.this.startActivity(mapsActivityIntent);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override

            public boolean onMove( RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target ) {
                return false;
            }

            @Override
            public void onSwiped( RecyclerView.ViewHolder viewHolder, int direction ) {
                final int position = viewHolder.getAdapterPosition(); //get position which is swipe
                AlertDialog.Builder builder = new AlertDialog.Builder(activity_open_plan.this);
                builder.setMessage("Are you sure to delete?");    //set message

                builder.setPositiveButton("REMOVE", new DialogInterface.OnClickListener() { //when click on DELETE
                    public void onClick(DialogInterface dialog, int which) {
                        adapter.notifyItemRemoved(position);    //item removed from recylcerview
                        Log.d("plan to remove spot", plans.get(position));
                        File dirToRemove = new File(getExternalFilesDir(null).getAbsolutePath().concat("/Plans"), plans.get(position));
                        Log.d("remove plan", "Removing plan " + dirToRemove.getAbsolutePath());
                        Log.d("remove plan", "Actual directory " + plans.get(position));
                        plans.remove(position);
                        deleteRecursive(dirToRemove);
                    }
                }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {  //not removing items if cancel is done
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        adapter.notifyItemRemoved(position + 1);    //notifies the RecyclerView Adapter that data in adapter has been removed at a particular position.
                        adapter.notifyItemRangeChanged(position, adapter.getItemCount());   //notifies the RecyclerView Adapter that positions of element in adapter has been changed from position(removed element index to end of list), please update it.
                    }
                }).show();  //show alert dialog
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        recyclerView.setAdapter(adapter);
    }
    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        if(fileOrDirectory.delete()){
            Log.d("Deleting Directory","Directory is deleted");
        }
    }

}
