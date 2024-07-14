package com.mct.app.helper.demo.native_rcv.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.mct.app.helper.demo.R;
import com.mct.app.helper.demo.native_rcv.User;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private final int layoutRes;
    private final ItemTouchHelper touchHelper;
    private final List<User> users;

    public UserAdapter(int layoutRes, ItemTouchHelper touchHelper, List<User> users) {
        this.layoutRes = layoutRes;
        this.touchHelper = touchHelper;
        this.users = users;
    }

    public List<User> getUsers() {
        return users;
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
        holder.tvName.setOnClickListener(v -> {

        });
        holder.tvName.setOnClickListener(v -> {
            String name = user.getName();
            String reg = "(_lClick_\\d+)$";
            Matcher matcher = Pattern.compile(reg).matcher(name);
            int id;
            if (matcher.find()) {
                name = name.substring(0, matcher.start());
                id = Integer.parseInt(matcher.group().replaceFirst("^(_lClick_)", "")) + 1;
            } else {
                id = 0;
            }
            user.setName(name + "_lClick_" + id);
            notifyItemChanged(holder.getAdapterPosition());
        });
        holder.imgAvatar.setOnLongClickListener(v -> {
            users.remove(user);
            notifyItemRemoved(holder.getAdapterPosition());
            return true;
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (touchHelper != null) {
                touchHelper.startDrag(holder);
            }
            return true;
        });
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
