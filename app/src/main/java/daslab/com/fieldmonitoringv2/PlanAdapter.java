package daslab.com.fieldmonitoringv2;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

class PlanAdapter extends RecyclerView.Adapter<PlanAdapter.PlanViewHolder> {

    private List<String> planList;

    PlanAdapter( List<String> plans, Context applicationContext ) {
        this.planList = plans;
    }

    @NonNull
    @Override
    public PlanAdapter.PlanViewHolder onCreateViewHolder( @NonNull ViewGroup parent, int viewType ) {
        View v= LayoutInflater.from(parent.getContext()).inflate(R.layout.layout,parent,false);
        PlanViewHolder viewHolder=new PlanViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder( @NonNull PlanViewHolder holder, int position ) {
        holder.plan.setText(planList.get(position));
    }

    @Override
    public int getItemCount() {
        return planList.size();
    }

    public static class PlanViewHolder extends RecyclerView.ViewHolder{
        protected TextView plan;
        public PlanViewHolder( View itemView ) {
            super(itemView);
            plan = (TextView) itemView.findViewById(R.id.planID);
        }
    }
    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

        @Override
        public boolean onMove( RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target ) {

            return false;
        }

        @Override
        public void onSwiped( RecyclerView.ViewHolder viewHolder, int direction ) {
            Log.d("itemPos",Long.toString(getItemId(viewHolder.getAdapterPosition())));
        }
    };
    ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);

    public ItemTouchHelper getItemTouchHelper() {
        return itemTouchHelper;
    }
}
