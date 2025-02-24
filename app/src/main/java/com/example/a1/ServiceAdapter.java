package com.example.a1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class ServiceAdapter extends ArrayAdapter<Service> {

    private Context context;
    private List<Service> services;

    public ServiceAdapter(Context context, List<Service> services) {
        super(context, R.layout.service_item, services);
        this.context = context;
        this.services = services;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.service_item, parent, false);
        }

        TextView serviceNameTextView = convertView.findViewById(R.id.serviceNameTextView);
        TextView servicePriceTextView = convertView.findViewById(R.id.servicePriceTextView);
        TextView serviceCategoryTextView = convertView.findViewById(R.id.serviceCategoryTextView);

        Service service = services.get(position);

        // Set values in TextViews
        serviceNameTextView.setText(service.getServiceName());
        servicePriceTextView.setText(String.format("$%.2f", service.getPrice()));
        serviceCategoryTextView.setText("Категория: " + service.getCategory());

        return convertView;
    }
}