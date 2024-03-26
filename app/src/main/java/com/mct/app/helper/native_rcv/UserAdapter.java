package com.mct.app.helper.native_rcv;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mct.app.helper.R;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private final int layoutRes;
    private final List<User> users;

    public UserAdapter(int layoutRes, List<User> users) {
        this.layoutRes = layoutRes;
        this.users = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(inflater.inflate(layoutRes, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        if (user == null) {
            return;
        }
        holder.tvName.setText(user.getName());
    }

    @Override
    public int getItemCount() {
        return users == null ? 0 : users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgAvatar;
        TextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.img_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
        }
    }
}
