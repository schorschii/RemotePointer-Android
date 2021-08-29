package systems.sieber.remotespotlight;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class ControlComputerAdapter extends BaseAdapter {

    private final Context context;
    private final List<ControlComputer> computers;

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
        View view;

        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_detail, parent, false);
        } else {
            view = convertView;
        }

        TextView text1 = view.findViewById(R.id.textViewItemTitle);
        TextView text2 = view.findViewById(R.id.textViewItemContent);
        text1.setText(computers.get(position).hostname);
        text2.setText(computers.get(position).address);

        return view;
    }

}
