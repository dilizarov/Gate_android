package com.unlock.gate;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.unlock.gate.adapters.NetworksListAdapter;
import com.unlock.gate.models.Network;
import com.unlock.gate.utils.APIRequestManager;
import com.unlock.gate.utils.Butter;
import com.unlock.gate.utils.RegexConstants;
import com.unlock.gate.utils.VolleyErrorHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * A fragment representing a list of Items.
 * <p/>
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class NetworksFragment extends ListFragment implements OnRefreshListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_POSITION = "position";
    private ListView networksList;
    private Button createNetwork;
    private NetworksListAdapter listAdapter;
    private ArrayList<Network> networks;
    private ArrayList<Network> adapterNetworks;
    private SharedPreferences mSessionPreferences;
    private PullToRefreshLayout mPullToRefreshLayout;
    private Button viewAggregate;

    private TextView noGatesMessage;
    private LinearLayout progressBarHolder;

    private final int CREATE_NETWORK_INTENT = 1;

    private int position;

    //private OnFragmentInteractionListener mListener;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public NetworksFragment() {
    }

    // TODO: Rename and change types of parameters
    public static NetworksFragment newInstance(int position) {
        NetworksFragment fragment = new NetworksFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            position = getArguments().getInt(ARG_POSITION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_networks, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewGroup viewGroup = (ViewGroup) view;
        mPullToRefreshLayout = new PullToRefreshLayout(viewGroup.getContext());

        ActionBarPullToRefresh.from(getActivity())
                .insertLayoutInto(viewGroup)
                .theseChildrenArePullable(getListView(), getListView().getEmptyView())
                .listener(this)
                .setup(mPullToRefreshLayout);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSessionPreferences = this.getActivity().getSharedPreferences(
                getString(R.string.session_shared_preferences_key), Context.MODE_PRIVATE);

        networksList = getListView();

        setListViewItemClickListeners();

        networks = new ArrayList<Network>();
        adapterNetworks = new ArrayList<Network>();

        noGatesMessage = (TextView) this.getActivity().findViewById(R.id.noGatesMessage);
        progressBarHolder = (LinearLayout) this.getActivity().findViewById(R.id.networkProgressBarHolder);

        createNetwork = (Button) this.getActivity().findViewById(R.id.createNetwork);

        viewAggregate = (Button) this.getActivity().findViewById(R.id.viewAggregate);
        viewAggregate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewFeedForNetwork(null);
            }
        });

        if (savedInstanceState != null) {
            networks = savedInstanceState.getParcelableArrayList("networkItems");

            progressBarHolder.setVisibility(View.GONE);
            adaptNewGatesToList();
        } else {
            requestNetworksAndPopulateListView(false);
        }

        setCreateNetworkClickListener();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */

    @Override
    public void onRefreshStarted(View view) {
        requestNetworksAndPopulateListView(true);
    }

    private void setListViewItemClickListeners() {
        networksList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                viewFeedForNetwork(networks.get(position));
            }
        });

        networksList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                                           long id) {

                final int networkIndex = position;

                // As Gate grows, we'll add more to this that could be done.
                final CharSequence[] items = {
                        "Leave " + networks.get(networkIndex).getName()
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        switch (item) {
                            case 0:
                                dialog.dismiss();
                                AlertDialog.Builder buildConfirmation = new AlertDialog.Builder(getActivity());
                                buildConfirmation.setMessage(getString(R.string.confirm_delete_network_message))
                                        .setPositiveButton(getString(R.string.yes_caps), new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialogConfirm, int item) {
                                                dialogConfirm.dismiss();
                                                //TODO: Once HQ is complete, make sure to also refresh that so keys are properly updated.

                                                leaveNetwork(networks.get(networkIndex));
                                            }
                                        })
                                        .setNegativeButton(getString(R.string.no_caps), new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialogConfirm, int item) {
                                                dialogConfirm.dismiss();
                                            }
                                        });

                                AlertDialog confirmation = buildConfirmation.create();
                                confirmation.show();

                                break;
                        }
                    }
                });

                AlertDialog alert = builder.create();
                alert.show();

                return true;
            }
        });
    }

    private void requestNetworksAndPopulateListView(final boolean refreshing) {
        try {

            JSONObject params = new JSONObject();
            params.put("user_id", mSessionPreferences.getString(getString(R.string.user_id_key), null))
                  .put("auth_token", mSessionPreferences.getString(getString(R.string.user_auth_token_key), null));

            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(final JSONObject response) {

                    new Thread(new Runnable() {

                        public void run() {
                            networks.clear();

                            JSONArray jsonNetworks = response.optJSONArray("networks");
                            int len = jsonNetworks.length();
                            for (int i = 0; i < len; i++) {
                                JSONObject jsonNetwork = jsonNetworks.optJSONObject(i);
                                Network network = new Network(jsonNetwork.optString("external_id"),
                                        jsonNetwork.optString("name"),
                                        jsonNetwork.optInt("users_count"),
                                        jsonNetwork.optJSONObject("creator").optString("name"));

                                networks.add(network);
                            }

                            getActivity().runOnUiThread(new Runnable() {
                                public void run() {
                                    if (refreshing) {
                                        mPullToRefreshLayout.setRefreshComplete();
                                        Butter.down(getActivity(), "Refreshed");
                                    } else {
                                        progressBarHolder.setVisibility(View.GONE);
                                    }

                                    adaptNewGatesToList();
                                }
                            });
                        }

                    }).start();
                }
            };

            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (refreshing) mPullToRefreshLayout.setRefreshComplete();
                    progressBarHolder.setVisibility(View.GONE);

                    VolleyErrorHandler volleyError = new VolleyErrorHandler(error);
                    Butter.down(getActivity(), volleyError.getMessage());
                }
            };

            APIRequestManager.getInstance().doRequest().getNetworks(params, listener, errorListener);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    private void leaveNetwork(final Network network) {
        networks.remove(network);
        adaptNewGatesToList();
        Butter.down(getActivity(), "Left " + network.getName());

        try {

            JSONObject params = new JSONObject();
            params.put("user_id", mSessionPreferences.getString(getString(R.string.user_id_key), null))
                    .put("auth_token", mSessionPreferences.getString(getString(R.string.user_auth_token_key), null));

            Response.Listener listener = new Response.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    //Don't do anything. Eagerly did actions assuming the request succeeds.
                }
            };

            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                    ArrayList<Network> failedNetworks = new ArrayList<Network>();
                    failedNetworks.add(network);
                    addNetworksToArrayList(failedNetworks);
                    adaptNewGatesToList();

                    Butter.down(getActivity(), volleyError.getMessage());
                }
            };

            APIRequestManager.getInstance().doRequest().leaveNetwork(network, params, listener, errorListener);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    private void setCreateNetworkClickListener() {
        createNetwork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CreateNetworkActivity.class);
                startActivityForResult(intent, CREATE_NETWORK_INTENT);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CREATE_NETWORK_INTENT:
                if (resultCode == getActivity().RESULT_OK) {
                    final String networkName = data.getStringExtra("networkName");

                    try {
                        JSONObject params = new JSONObject();
                        params.put("user_id", mSessionPreferences.getString(getString(R.string.user_id_key), null))
                              .put("auth_token", mSessionPreferences.getString(getString(R.string.user_auth_token_key), null));

                        JSONObject network = new JSONObject();
                        network.put("name", networkName.replaceAll(RegexConstants.SPACE_NEW_LINE, " "));

                        params.put("network", network);

                        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                JSONObject jsonNetwork = response.optJSONObject("network");
                                final Network network = new Network(jsonNetwork.optString("external_id"),
                                        jsonNetwork.optString("name"),
                                        1,
                                        jsonNetwork.optJSONObject("creator").optString("name"));

                                ArrayList<Network> newNetworks = new ArrayList<Network>();
                                newNetworks.add(network);

                                addNetworksToArrayList(newNetworks);
                                adaptNewGatesToList();
                            }
                        };

                        Response.ErrorListener errorListener = new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                                Intent intent = new Intent(getActivity(), CreateNetworkActivity.class);
                                intent.putExtra("networkName", networkName);

                                if (volleyError.isExpectedError()) {
                                    intent.putExtra("errors", volleyError.getPrettyErrors());
                                } else {
                                    intent.putExtra("errorMessage", volleyError.getMessage());
                                }

                                startActivityForResult(intent, CREATE_NETWORK_INTENT);
                            }
                        };

                        APIRequestManager.getInstance().doRequest().createNetwork(params, listener, errorListener);
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList("networkItems", networks);
    }

    public void viewFeedForNetwork(Network network) {
        ((MainActivity) getActivity()).showFeed(network, false, true);
    }

    public ArrayList<Network> getNetworks() {
        return networks;
    }

    public void addNetworksToArrayList(final ArrayList<Network> newNetworks) {

        // networkItems is a sorted array. newNetworks will be a very small array.
        // Ultimately, this shouldn't take long at all, but if for some reason we see it
        // lagging, then we could speed this up with another algorithm.

        int len = newNetworks.size();
        int startingPoint = 0;
        boolean reachedEnd = false;
        for (int i = 0; i < len; i++) {
            Network network = newNetworks.get(i);

            int length = networks.size();

            if (length == 0) {
                networks.add(network);
                continue;
            }

            for (int j = startingPoint; j < length; j++) {
                String name = networks.get(j).getName();
                if (name.compareToIgnoreCase(network.getName()) > 0) {
                    networks.add(j, network);
                    startingPoint = j + 1;
                    break;
                } else if (reachedEnd || j == length - 1) {
                    networks.add(network);
                    reachedEnd = true;
                    break;
                }
            }
        }
    }

    public void adaptNewGatesToList() {

        noGatesMessage.setVisibility(
                networks.size() == 0
                ? View.VISIBLE
                : View.GONE
        );

        if (listAdapter == null) {
            listAdapter = new NetworksListAdapter(getActivity(), adapterNetworks);
            networksList.setAdapter(listAdapter);
        }

        adapterNetworks.clear();
        adapterNetworks.addAll(networks);
        listAdapter.notifyDataSetChanged();
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(String id);
    }

}