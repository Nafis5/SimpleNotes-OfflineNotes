package com.notes.keepnotes;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
    LayoutInflater layoutInflater;
    List<Note> notes;
    MainActivity mainActivity;




    Adapter(Context context,List<Note> notes){
        this.layoutInflater=LayoutInflater.from(context);
        this.notes=notes;
        mainActivity=(MainActivity) context;

    }
    @NonNull
    @Override
    public Adapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view=layoutInflater.inflate(R.layout.custom_list_view,parent,false);


        if(TextSizeUtil.getScreenSize(mainActivity)>7.0){
            view=layoutInflater.inflate(R.layout.custom_list_view_large,parent,false);
        }




        return new ViewHolder(view,mainActivity);
    }

    @Override
    public void onBindViewHolder(@NonNull Adapter.ViewHolder holder, int position) {
        String title=notes.get(position).getTitle();
        String date=notes.get(position).getDate();
        String time=notes.get(position).getTime();
        holder.ntitle.setText(title);
        holder.nDate.setText(date);
        holder.nTime.setText(time);
        if(!mainActivity.is_in_action_mode){
            holder.checkBox.setVisibility(View.GONE);

        }
        else{
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.checkBox.setChecked(false);
        }


    }

    @Override
    public int getItemCount() {
        try {
            return notes.size();
        } catch (Exception e) {
            return 0;
        }
    }
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView ntitle,nDate,nTime;
        CheckBox checkBox;
        MainActivity mainActivity;

        public ViewHolder(@NonNull View itemView, final MainActivity mainActivity) {
            super(itemView);
            ntitle=itemView.findViewById(R.id.nTitle);
            nDate=itemView.findViewById(R.id.nDate);
            nTime=itemView.findViewById(R.id.nTime);
            checkBox=(CheckBox) itemView.findViewById(R.id.check);
            this.mainActivity=mainActivity;
            checkBox.setOnClickListener(this);

            itemView.setOnLongClickListener(mainActivity);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {


                    if(!mainActivity.is_in_action_mode) {



                        Intent i = new Intent(v.getContext(), Details.class);
                        i.putExtra("ID", notes.get(getAdapterPosition()).getId());
                        v.getContext().startActivity(i);
                    }
                }
            });
        }


        @Override
        public void onClick(View v) {

            mainActivity.prepareSelection(v,notes.get(getAdapterPosition()).getId());
        }

    }
    public void updateAdapter(List<Note> updateslist){
       this.notes=updateslist;
        notifyDataSetChanged();

    }

}
