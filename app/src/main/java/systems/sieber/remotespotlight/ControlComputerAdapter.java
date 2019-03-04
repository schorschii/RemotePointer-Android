package systems.sieber.remotespotlight;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import java.util.List;

public class ControlComputerAdapter extends BaseAdapter {

    private Context context;
    private List<ControlComputer> computers;

    ControlComputerAdapter(Context context, List<ControlComputer> computers) {
        this.context = context;
        this.computers = computers;
    }

    @Override
    public int getCount() {
        return computers.size();
    }

    @Override
    public Object getItem(int position) {
        return computers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        TwoLineListItem twoLineListItem;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            twoLineListItem = (TwoLineListItem) inflater.inflate(
                    android.R.layout.simple_list_item_2, null);
        } else {
            twoLineListItem = (TwoLineListItem) convertView;
        }

        TextView text1 = twoLineListItem.getText1();
        TextView text2 = twoLineListItem.getText2();

        text1.setText(computers.get(position).hostname);
        text2.setText(computers.get(position).address);
        text2.setTextColor(Color.argb(100,0,0,0));

        return twoLineListItem;
    }

}
