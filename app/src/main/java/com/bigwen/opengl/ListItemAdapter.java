package com.bigwen.opengl;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;

public class ListItemAdapter extends BaseAdapter {

    private List<String> objects = new ArrayList<String>();

    private Context context;
    private LayoutInflater layoutInflater;
    private ItemCallback itemCallback;

    public ListItemAdapter(Context context) {
        this.context = context;
        this.layoutInflater = LayoutInflater.from(context);
    }

    public void setObjects(List<String> objects) {
        this.objects.clear();
        this.objects.addAll(objects);
        notifyDataSetChanged();
    }

    public void setItemCallback(ItemCallback itemCallback) {
        this.itemCallback = itemCallback;
    }

    @Override
    public int getCount() {
        return objects.size();
    }

    @Override
    public String getItem(int position) {
        return objects.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_item, null);
            convertView.setTag(new ViewHolder(convertView));
        }
        initializeViews(getItem(position), (ViewHolder) convertView.getTag());
        return convertView;
    }

    private void initializeViews(final String object, ViewHolder holder) {
        holder.btn.setText(object);
        holder.btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemCallback != null) itemCallback.onClick(object);
            }
        });
    }

    protected class ViewHolder {
        private Button btn;

        public ViewHolder(View view) {
            btn = (Button) view.findViewById(R.id.btn);
        }
    }

    public interface ItemCallback {
        void onClick(String data);
    }
}
