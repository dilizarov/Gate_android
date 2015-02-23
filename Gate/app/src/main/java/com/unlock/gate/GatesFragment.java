package com.unlock.gate;

import android.animation.ValueAnimator;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.gc.materialdesign.views.ButtonFloat;
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

import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.DefaultHeaderTransformer;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * A fragment representing a list of Items.
 */
public class GatesFragment extends ListFragment implements OnRefreshListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_POSITION = "position";
    private ListView gatesList;
    private ButtonFloat createGate;
    private GatesListAdapter listAdapter;
    private ArrayList<Gate> gates;
    private ArrayList<Gate> adapterGates;
    private PullToRefreshLayout mPullToRefreshLayout;
    private Button viewAggregate;

    private TextView noGatesMessage;
    private LinearLayout progressBarHolder;

    private ProgressBar gateLoading;

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
    public void onResume() {
        super.onResume();

        DefaultHeaderTransformer transformer = (DefaultHeaderTransformer) mPullToRefreshLayout
                .getHeaderTransformer();
        transformer.getHeaderView().findViewById(R.id.ptr_text)
                .setBackgroundColor(getResources().getColor(R.color.black));

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
        mPullToRefreshLayout = new PullToRefreshLayout(view.getContext());

        ActionBarPullToRefresh.from(getActivity())
                .insertLayoutInto(viewGroup)
                .theseChildrenArePullable(getListView(), getListView().getEmptyView())
                .listener(this)
                .setup(mPullToRefreshLayout);

        DefaultHeaderTransformer transformer = (DefaultHeaderTransformer) mPullToRefreshLayout
                .getHeaderTransformer();
        transformer.getHeaderView().findViewById(R.id.ptr_text)
                .setBackgroundColor(getResources().getColor(R.color.black));
        transformer.setProgressBarColor(getResources().getColor(R.color.white));
        transformer.setProgressBarHeight(2);
        transformer.setPullText("Pull down the internet...");
        transformer.setRefreshingText("Finding Gates...");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        gatesList = getListView();

        setListViewItemClickListeners();

        gates = new ArrayList<Gate>();
        adapterGates = new ArrayList<Gate>();

        noGatesMessage = (TextView) this.getActivity().findViewById(R.id.noGatesMessage);
        progressBarHolder = (LinearLayout) this.getActivity().findViewById(R.id.gateProgressBarHolder);

        gateLoading = (ProgressBar) this.getActivity().findViewById(R.id.gateLoading);

        createGate = (ButtonFloat) this.getActivity().findViewById(R.id.createGate);

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

                new MaterialDialog.Builder(getActivity())
                        .title(gates.get(gateIndex).getName())
                        .items(items)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                switch (which) {
                                    case 0:
                                        new MaterialDialog.Builder(getActivity())
                                                .title(gates.get(gateIndex).getName())
                                                .content(R.string.confirm_delete_gate_message)
                                                .positiveText(R.string.yes_caps)
                                                .negativeText(R.string.no_caps)
                                                .callback(new MaterialDialog.ButtonCallback() {
                                                    @Override
                                                    public void onPositive(MaterialDialog dialog) {
                                                        dialog.dismiss();

                                                        leaveGate(gates.get(gateIndex));
                                                    }

                                                    @Override
                                                    public void onNegative(MaterialDialog dialog) {
                                                        dialog.dismiss();
                                                    }
                                                }).show();
                                        break;
                                }
                            }
                        }).show();

                return true;
            }
        });
    }

    private void requestGatesAndPopulateListView(final boolean refreshing) {

        JSONObject params = new JSONObject();

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

                        if (getActivity() == null) return;
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

                if (gates.size() == 0) {
                    if (volleyError.isConnectionError())
                        noGatesMessage.setText(R.string.volley_no_connection_error);
                    else
                        noGatesMessage.setText(R.string.gate_error_message);

                    noGatesMessage.setVisibility(View.VISIBLE);
                }

                Butter.down(getActivity(), volleyError.getMessage());
            }
        };

        APIRequestManager.getInstance().doRequest().getGates(params, listener, errorListener);
    }

    private void leaveGate(final Gate gate) {
        gates.remove(gate);
        adaptNewGatesToList();
        Butter.down(getActivity(), "Left " + gate.getName());

        JSONObject params = new JSONObject();

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

    }

    private void showCreateGateDialog(String attemptedGateName) {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.title_dialog_create_gate)
                .customView(R.layout.dialog_create_gate, true)
                .positiveText("CREATE")
                .negativeText("CANCEL")
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        dialog.dismiss();

                        EditText input = (EditText) dialog.getCustomView().findViewById(R.id.createGateName);
                        String gateName = input.getText().toString().trim();

                        createGate(gateName);
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        dialog.dismiss();
                    }
                })
                .showListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {

                    }
                }).build();

        final View positiveAction = dialog.getActionButton(DialogAction.POSITIVE);
        EditText gateNameInput = (EditText) dialog.getCustomView().findViewById(R.id.createGateName);

        gateNameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                positiveAction.setEnabled(s.toString().trim().length() > 0);
            }
        });

        if (attemptedGateName != null && attemptedGateName.length() > 0) {
            gateNameInput.append(attemptedGateName);
            positiveAction.setEnabled(true);
        } else {
            positiveAction.setEnabled(false);
        }

        dialog.show();
    }

    private void setCreateGateClickListener() {
        createGate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateGateDialog(null);
            }
        });
    }

    public void createGate(final String gateName) {
        expandCreatedGateLoading();

        try {
            JSONObject params = new JSONObject();

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

                    final int index = gates.indexOf(gate);

                    gatesList.post(new Runnable() {
                        @Override
                        public void run() {
                            gatesList.setSelection(index);
                        }
                    });

                    adaptNewGatesToList();

                    gatesList.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            collapseCreatedGateLoading();
                        }
                    }, 200);
                }
            };

            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                    Butter.down(getActivity(), volleyError.getMessage());

                    collapseCreatedGateLoading();

                    showCreateGateDialog(gateName);
                }
            };

            APIRequestManager.getInstance().doRequest().createGate(params, listener, errorListener);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList("gateItems", gates);
    }

    public void viewFeedForGate(Gate gate) {
        ((MainActivity) getActivity()).showFeed(gate, true);
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

    public void expandCreatedGateLoading() {
        gateLoading.setVisibility(View.VISIBLE);
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        gateLoading.measure(widthSpec, heightSpec);

        ValueAnimator animator = slideAnimator(0, gateLoading.getMeasuredHeight(), gateLoading);
        animator.start();
    }

    public void collapseCreatedGateLoading() {
        int finalHeight = gateLoading.getHeight();

        ValueAnimator animator = slideAnimator(finalHeight, 0, gateLoading);
        animator.start();
    }

    public static ValueAnimator slideAnimator(int start, int end, final ProgressBar gateLoading) {

        ValueAnimator animator = ValueAnimator.ofInt(start, end);

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                int value = (Integer) animation.getAnimatedValue();

                ViewGroup.LayoutParams layoutParams = gateLoading.getLayoutParams();
                layoutParams.height = value;
                gateLoading.setLayoutParams(layoutParams);
            }
        });

        return animator;
    }
}