package com.unlock.gate;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.unlock.gate.adapters.NetworksListAdapter;
import com.unlock.gate.models.Network;
import com.unlock.gate.utils.APIRequestManager;
import com.unlock.gate.utils.VolleyErrorHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * A fragment representing a list of Items.
 * <p />
 * <p />
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class NetworksFragment extends ListFragment implements OnRefreshListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_POSITION = "position";
    private ListView networks;
    private NetworksListAdapter listAdapter;
    private List<Network> networkItems;
    private SharedPreferences mSessionPreferences;
    private ProgressBar loadingNetworksProgressBar;
    private PullToRefreshLayout mPullToRefreshLayout;

    private int position;

    //private OnFragmentInteractionListener mListener;

    // TODO: Rename and change types of parameters
    public static NetworksFragment newInstance(int position) {
        NetworksFragment fragment = new NetworksFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public NetworksFragment() {
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

        try {
            mSessionPreferences = this.getActivity().getSharedPreferences(
                    getString(R.string.session_shared_preferences_key), Context.MODE_PRIVATE);

            networks = getListView();

            networkItems = new ArrayList<Network>();
            listAdapter = new NetworksListAdapter(getActivity(), networkItems);
            networks.setAdapter(listAdapter);

            final LinearLayout progressBarHolder = (LinearLayout) this.getActivity().findViewById(R.id.networkProgressBarHolder);

            JSONObject params = new JSONObject();
            params.put("user_id", mSessionPreferences.getString(getString(R.string.user_id_key), null))
                    .put("auth_token", mSessionPreferences.getString(getString(R.string.user_auth_token_key), null));

            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                    final JSONObject jsonResponse = response;

                    new Thread(new Runnable() {

                        public void run() {
                            JSONArray jsonNetworks = jsonResponse.optJSONArray("networks");
                            int len = jsonNetworks.length();
                            for (int i = 0; i < len; i++) {
                                JSONObject jsonNetwork = jsonNetworks.optJSONObject(i);
                                Network network = new Network(jsonNetwork.optString("external_id"),
                                        jsonNetwork.optString("name"),
                                        jsonNetwork.optJSONObject("creator").optString("name"));

                                networkItems.add(network);
                            }

                            getActivity().runOnUiThread(new Runnable() {
                                public void run() {
                                    progressBarHolder.setVisibility(View.GONE);
                                    listAdapter.notifyDataSetChanged();
                                }
                            });
                        }

                    }).start();
                }
            };

            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                    Log.v("Fucked", mSessionPreferences.getString(getString(R.string.user_auth_token_key), "nope"));
                }
            };

            APIRequestManager.getInstance().doRequest().getNetworks(params, listener, errorListener);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }
    /**
    * This interface must be implemented by activities that contain this
    * fragment to allow an interaction in this fragment to be communicated
    * to the activity and potentially other fragments contained in that
    * activity.
    * <p>
    * See the Android Training lesson <a href=
    * "http://developer.android.com/training/basics/fragments/communicating.html"
    * >Communicating with Other Fragments</a> for more information.
    */

    @Override
    public void onRefreshStarted(View view) {
        new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(4000); } catch (InterruptedException e) {}

                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        mPullToRefreshLayout.setRefreshComplete();
                        Toast.makeText(getActivity(), "Refreshed", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(String id);
    }

}
