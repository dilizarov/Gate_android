package com.unlock.gate;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.unlock.gate.adapters.GatesListAdapter;
import com.unlock.gate.models.Gate;
import com.unlock.gate.utils.APIRequestManager;
import com.unlock.gate.utils.Butter;
import com.unlock.gate.utils.RegexConstants;
import com.unlock.gate.utils.VolleyErrorHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.DefaultHeaderTransformer;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * A fragment representing a list of Items.
 */
public class GatesFragment extends ListFragment implements OnRefreshListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_POSITION = "position";
    private ListView gatesList;
    private Button createGate;
    private GatesListAdapter listAdapter;
    private ArrayList<Gate> gates;
    private ArrayList<Gate> adapterGates;
    private SharedPreferences mSessionPreferences;
    private PullToRefreshLayout mPullToRefreshLayout;
    private Button viewAggregate;

    private TextView noGatesMessage;
    private LinearLayout progressBarHolder;

    private final int CREATE_GATE_INTENT = 1;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public GatesFragment() {
    }

    // TODO: Rename and change types of parameters
    public static GatesFragment newInstance() {
        GatesFragment fragment = new GatesFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_gates, container, false);
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

        DefaultHeaderTransformer transformer = (DefaultHeaderTransformer) mPullToRefreshLayout
                .getHeaderTransformer();
        transformer.getHeaderView().findViewById(R.id.ptr_text)
                .setBackgroundColor(getResources().getColor(R.color.black));
        transformer.setProgressBarColor(getResources().getColor(R.color.gate_blue));
        transformer.setPullText("Pull down the internet...");
        transformer.setRefreshingText("Finding Gates...");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSessionPreferences = this.getActivity().getSharedPreferences(
                getString(R.string.session_shared_preferences_key), Context.MODE_PRIVATE);

        gatesList = getListView();

        setListViewItemClickListeners();

        gates = new ArrayList<Gate>();
        adapterGates = new ArrayList<Gate>();

        noGatesMessage = (TextView) this.getActivity().findViewById(R.id.noGatesMessage);
        progressBarHolder = (LinearLayout) this.getActivity().findViewById(R.id.gateProgressBarHolder);

        createGate = (Button) this.getActivity().findViewById(R.id.createGate);

        viewAggregate = (Button) this.getActivity().findViewById(R.id.viewAggregate);
        viewAggregate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewFeedForGate(null);
            }
        });

        if (savedInstanceState != null) {
            gates = savedInstanceState.getParcelableArrayList("gateItems");

            progressBarHolder.setVisibility(View.GONE);
            adaptNewGatesToList();
        } else {
            requestGatesAndPopulateListView(false);
        }

        setCreateGateClickListener();
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
        requestGatesAndPopulateListView(true);
    }

    private void setListViewItemClickListeners() {
        gatesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                viewFeedForGate(gates.get(position));
            }
        });

        gatesList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                                           long id) {

                final int gateIndex = position;

                // As Gate grows, we'll add more to this that could be done.
                final CharSequence[] items = {
                        "Leave"
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(gates.get(gateIndex).getName())
                        .setItems(items, new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialog, int item) {
                               switch (item) {
                                   case 0:
                                       dialog.dismiss();
                                       AlertDialog.Builder buildConfirmation = new AlertDialog.Builder(getActivity());
                                       buildConfirmation.setTitle(gates.get(gateIndex).getName())
                                               .setMessage(getString(R.string.confirm_delete_gate_message))
                                               .setPositiveButton(getString(R.string.yes_caps), new DialogInterface.OnClickListener() {

                                                   @Override
                                                   public void onClick(DialogInterface dialogConfirm, int item) {
                                                       dialogConfirm.dismiss();
                                                       //TODO: Once HQ is complete, make sure to also refresh that so keys are properly updated.

                                                       leaveGate(gates.get(gateIndex));
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
                alert.setCanceledOnTouchOutside(true);
                alert.show();

                return true;
            }
        });
    }

    private void requestGatesAndPopulateListView(final boolean refreshing) {
        try {

            JSONObject params = new JSONObject();
            params.put("user_id", mSessionPreferences.getString(getString(R.string.user_id_key), null))
                  .put("auth_token", mSessionPreferences.getString(getString(R.string.user_auth_token_key), null));

            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(final JSONObject response) {

                    new Thread(new Runnable() {

                        public void run() {
                            gates.clear();

                            JSONArray jsonGates = response.optJSONArray("gates");
                            int len = jsonGates.length();
                            for (int i = 0; i < len; i++) {
                                JSONObject jsonGate = jsonGates.optJSONObject(i);
                                Gate gate = new Gate(jsonGate.optString("external_id"),
                                        jsonGate.optString("name"),
                                        jsonGate.optInt("users_count"),
                                        jsonGate.optJSONObject("creator").optString("name"));

                                gates.add(gate);
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
                    VolleyErrorHandler volleyError = new VolleyErrorHandler(error);
                    if (refreshing) mPullToRefreshLayout.setRefreshComplete();
                    progressBarHolder.setVisibility(View.GONE);

                    if (gates.size() == 0 && volleyError.isConnectionError()) {
                        noGatesMessage.setText(R.string.gate_error_message);
                        noGatesMessage.setVisibility(View.VISIBLE);
                    }

                    Butter.down(getActivity(), volleyError.getMessage());
                }
            };

            APIRequestManager.getInstance().doRequest().getGates(params, listener, errorListener);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    private void leaveGate(final Gate gate) {
        gates.remove(gate);
        adaptNewGatesToList();
        Butter.down(getActivity(), "Left " + gate.getName());

        try {

            JSONObject params = new JSONObject();
            params.put("user_id", mSessionPreferences.getString(getString(R.string.user_id_key), null))
                    .put("auth_token", mSessionPreferences.getString(getString(R.string.user_auth_token_key), null));

            Response.Listener<Integer> listener = new Response.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    //Don't do anything. Eagerly did actions assuming the request succeeds.
                }
            };

            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                    // We're just packing one Gate into failedGates because
                    // addGatesToArrayList takes in an ArrayList
                    ArrayList<Gate> failedGates = new ArrayList<Gate>();
                    failedGates.add(gate);
                    addGatesToArrayList(failedGates);
                    adaptNewGatesToList();

                    Butter.down(getActivity(), volleyError.getMessage());
                }
            };

            APIRequestManager.getInstance().doRequest().leaveGate(gate, params, listener, errorListener);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    private void setCreateGateClickListener() {
        createGate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CreateGateActivity.class);
                startActivityForResult(intent, CREATE_GATE_INTENT);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CREATE_GATE_INTENT:
                if (resultCode == getActivity().RESULT_OK) {
                    final String gateName = data.getStringExtra("gateName");

                    try {
                        JSONObject params = new JSONObject();
                        params.put("user_id", mSessionPreferences.getString(getString(R.string.user_id_key), null))
                              .put("auth_token", mSessionPreferences.getString(getString(R.string.user_auth_token_key), null));

                        JSONObject gate = new JSONObject();
                        gate.put("name", gateName.replaceAll(RegexConstants.SPACE_NEW_LINE, " "));

                        params.put("gate", gate);

                        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                JSONObject jsonGate = response.optJSONObject("gate");
                                final Gate gate = new Gate(jsonGate.optString("external_id"),
                                        jsonGate.optString("name"),
                                        1,
                                        jsonGate.optJSONObject("creator").optString("name"));

                                ArrayList<Gate> newGates = new ArrayList<Gate>();
                                newGates.add(gate);

                                addGatesToArrayList(newGates);
                                adaptNewGatesToList();
                            }
                        };

                        Response.ErrorListener errorListener = new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                                Intent intent = new Intent(getActivity(), CreateGateActivity.class);
                                intent.putExtra("gateName", gateName);

                                intent.putExtra("errorMessage", volleyError.getMessage());

                                startActivityForResult(intent, CREATE_GATE_INTENT);
                            }
                        };

                        APIRequestManager.getInstance().doRequest().createGate(params, listener, errorListener);
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList("gateItems", gates);
    }

    public void viewFeedForGate(Gate gate) {
        ((MainActivity) getActivity()).showFeed(gate, false, true);
    }

    public ArrayList<Gate> getGates() {
        return gates;
    }

    public void addGatesToArrayList(final ArrayList<Gate> newGates) {

        // gateItems is a sorted array. newGates will be a very small array.
        // Ultimately, this shouldn't take long at all, but if for some reason we see it
        // lagging, then we could speed this up with another algorithm.

        int len = newGates.size();
        int startingPoint = 0;
        boolean reachedEnd = false;
        for (int i = 0; i < len; i++) {
            Gate gate = newGates.get(i);

            int length = gates.size();

            if (length == 0) {
                gates.add(gate);
                continue;
            }

            for (int j = startingPoint; j < length; j++) {
                String name = gates.get(j).getName();
                if (name.compareToIgnoreCase(gate.getName()) > 0) {
                    gates.add(j, gate);
                    startingPoint = j + 1;
                    break;
                } else if (reachedEnd || j == length - 1) {
                    gates.add(gate);
                    reachedEnd = true;
                    break;
                }
            }
        }
    }

    public void adaptNewGatesToList() {

        noGatesMessage.setText(R.string.no_gates_default);
        noGatesMessage.setVisibility(
                gates.size() == 0
                ? View.VISIBLE
                : View.GONE
        );

        if (listAdapter == null) {
            listAdapter = new GatesListAdapter(getActivity(), adapterGates);
            gatesList.setAdapter(listAdapter);
        }

        adapterGates.clear();
        adapterGates.addAll(gates);
        listAdapter.notifyDataSetChanged();
    }

}