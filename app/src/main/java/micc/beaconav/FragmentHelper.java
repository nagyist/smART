package micc.beaconav;


import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentManager;
//import android.app.FragmentManager;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.TypedValue;
import android.view.View;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.LinkedList;
import java.util.List;

import micc.beaconav.db.dbHelper.DbManager;
import micc.beaconav.db.dbHelper.IArtRow;
import micc.beaconav.db.dbHelper.museum.MuseumRow;
import micc.beaconav.db.dbJSONManager.JSONHandler;
import micc.beaconav.fragments.backPressedListeners.OnBackPressedListener;
import micc.beaconav.fragments.slidingContentFragment.slidingContentList.ArtListFragment;
import micc.beaconav.fragments.slidingContentFragment.slidingContentDescription.ArtworkDescrFragment;
import micc.beaconav.fragments.slidingContentFragment.slidingContentDescription.MuseumDescrFragment;
import micc.beaconav.fragments.slidingHeaderFragment.NameHeaderFragment;
import micc.beaconav.fragments.slidingHeaderFragment.SeekBarHeaderFragment;
import micc.beaconav.gui.animationHelper.DpHelper;
import micc.beaconav.indoorEngine.ArtworkPosition;
import micc.beaconav.indoorEngine.ArtworkRow;
import micc.beaconav.indoorEngine.IndoorMap;
import micc.beaconav.indoorEngine.IndoorMapFragmentLite;
import micc.beaconav.indoorEngine.building.Building;
import micc.beaconav.indoorEngine.building.Position;
import micc.beaconav.outdoorEngine.Map;
import micc.beaconav.outdoorEngine.MapFragment;
import micc.beaconav.outdoorEngine.MuseumMarkerManager;

/**
 * Created by Riccardo Del Chiaro & Franco Yang (25/02/2015)
 */



//A questo punto per ogni Fragment sparato c'è un'imlementazione di OnBackPressedListener che spara il fragment precedente

public class FragmentHelper  implements MuseumMarkerManager
{

    private static FragmentHelper istance = null;
    private static MainActivity mainActivity = null;

    public static FragmentHelper instance(){
        if(istance == null) istance = new FragmentHelper();
        return istance;
    }

    private FragmentHelper() {

    }

    public static void setMainActivity(MainActivity activity) {
        // if(mainActivity == null) { // NO!!!! IN QUESTO MODO chiudendo e riaprendo l'app non cambio l'address della main activiti -> force close al primo fragment transaction !!!
            mainActivity = activity;
            mainActivity.setOnBackPressedListener(instance().getOnBackPressedListener());
        //}
        // settabile solo 1 volta
    }



    public enum SlidingFragment { LIST, DETAILS, NAVIGATE }
    public enum MainFragment{ OUTDOOR, INDOOR; }



    private MainFragment    activeMainFragment    = null;
    private SlidingFragment activeSlidingFragment = null;


    public MapFragment       mapFragment = new MapFragment();
    public IndoorMapFragmentLite indoorMapFragmentLite = null;


    public ArtListFragment museumListFragment = new ArtListFragment();
    public ArtListFragment artworkListFragment = new ArtListFragment();
    public MuseumRow artworkList_museumRow = null;

    public MuseumDescrFragment museumDescrFragment = new MuseumDescrFragment();
    public ArtworkDescrFragment artworkDescrFragment = new ArtworkDescrFragment();
    public SeekBarHeaderFragment seekBarHeaderFragment = new SeekBarHeaderFragment();
    public NameHeaderFragment nameHeaderFragment = new NameHeaderFragment();





    public OnBackPressedListener getOnBackPressedListener(){
        return new OnBackPressedListener() {


            @Override
            public void doBack() {
                if(activeMainFragment == MainFragment.OUTDOOR)
                {
                    switch(activeSlidingFragment){
                        case NAVIGATE:
                            MuseumRow selectedMuseumRow = mapFragment.getMap().getSelectedMuseumRow();
                            simulateDeselectMuseumOnMapClick();
                            simulateMuseumOnMapClick(selectedMuseumRow);
                            break;

                        case DETAILS:
                            simulateDeselectMuseumOnMapClick();
                            showMuseumListFragment();
                            break;

                        case LIST:
                            switch (mainActivity.getSlidingUpPanelLayout().getPanelState())
                            {
                                case EXPANDED:
                                    mainActivity.getSlidingUpPanelLayout().setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
                                    break;

                                case ANCHORED:
                                    mainActivity.getSlidingUpPanelLayout().setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                                    break;

                                case COLLAPSED:
                                {
                                    if (mainActivity.getSlidingUpPanelLayout().getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                                        AlertDialog alertDialog = new AlertDialog.Builder(mainActivity).create();

                                        alertDialog.setTitle("Action confirm request");
                                        alertDialog.setMessage("Do you want to close the application?");

                                        // Setting Icon to Dialog
                                        //alertDialog.setIcon(R.drawable.);

                                        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                showOutdoorFragment();
                                                mainActivity.finish();
                                                //System.exit(0);
                                            }
                                        });

                                        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        });

                                        alertDialog.show();
                                    }
                                    break;
                                }
                            }
                            break;

                    }

                }
                else if(activeMainFragment == MainFragment.INDOOR)
                {
                    switch(activeSlidingFragment){
                        case NAVIGATE:
                            instance().indoorMapFragmentLite.getIndoorMap().hideDijkstraPath();
                            break;

                        case DETAILS:
                            showArtworkListFragment(indoorMapFragmentLite.getMuseumRow(), indoorMapFragmentLite.getBuilding());
                            indoorMapFragmentLite.getIndoorMap().onMarkerSpotSelected(null);
                            break;

                        case LIST:
                            switch (mainActivity.getSlidingUpPanelLayout().getPanelState())
                            {
                                case EXPANDED:
                                    mainActivity.getSlidingUpPanelLayout().setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
                                    break;

                                case ANCHORED:
                                    mainActivity.getSlidingUpPanelLayout().setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                                    break;

                                case COLLAPSED:
                                {
                                    if (mainActivity.getSlidingUpPanelLayout().getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                                        AlertDialog alertDialog = new AlertDialog.Builder(mainActivity).create();

                                        alertDialog.setTitle("Action confirm request");
                                        alertDialog.setMessage("Do you want to come back to the outdoor map?");

                                        // Setting Icon to Dialog
                                        //alertDialog.setIcon(R.drawable.);

                                        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                showOutdoorFragment();
                                                indoorMapFragmentLite = null;
                                            }
                                        });

                                        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        });

                                        alertDialog.show();
                                    }
                                    break;
                                }
                            }                            break;

                    }
                }
            }

        };
    }


    public MainActivity getMainActivity() {
        return mainActivity;
    }


    public boolean isIndoorMode() {
        return activeMainFragment == MainFragment.INDOOR;
    }










    //+++++++++++++++++++++++++METODI SETUP PER LA VISUALIZZAIONE DEI FRAGMENT+++++++++++++++++++


    public final void showOutdoorFragment() {

        mainActivity.initUniversalImageLoader();
        //if(activeMainFragment != MainFragment.OUTDOOR) {
            activeMainFragment = MainFragment.OUTDOOR;
            indoorMapFragmentLite = null;

            swapFragment(R.id.fragment_map_container, mapFragment);

            //mapFragment.setMuseumMarkerManager(this);
            showMuseumListFragment();

            mainActivity.setFABListener(defaultFABOnClickListener);
            mainActivity.getFloatingActionButtonQRScanBtn().setVisibility(View.INVISIBLE);
            mainActivity.getFloatingActionButtonNotifyBeaconProximity().setVisibility(View.INVISIBLE);
            mainActivity.getFloatingActionButtonNotifyToIndoor().setVisibility(View.INVISIBLE);
        //}
    }


    public final void showIndoorFragment(MuseumRow museum) {
//        indoorMapFragment = new IndoorMapFragment();
//        swapFragment(R.id.fragment_map_container, indoorMapFragment);

        mainActivity.initUniversalImageLoader();

        activeMainFragment = MainFragment.INDOOR;
        showArtworkListFragment(museum, null);
        mainActivity.setThemeColor(MainActivity.ThemeColor.RED);
        mainActivity.getFloatingActionButton().setIconDrawable(mainActivity.getResources().getDrawable(R.drawable.white_museum));
        mainActivity.getFloatingActionButtonQRScanBtn().setVisibility(View.VISIBLE);
        mainActivity.getFloatingActionButtonNotifyToIndoor().setVisibility(View.INVISIBLE);

        indoorMapFragmentLite = new IndoorMapFragmentLite();
        indoorMapFragmentLite.setMuseum(museum);
        swapFragment(R.id.fragment_map_container, indoorMapFragmentLite);


//        indoorMapFragment.setMuseum(museum);
    }



    public final void showArtworkListFragment(MuseumRow museum, Building building) {

        LinkedList<IArtRow> artworkRows = new LinkedList<>();
        if(building != null)
        {
            List<Position> positions = building.getAllPositions();
            for(Position p : positions)
            {
                if( p instanceof ArtworkPosition)
                    artworkRows.addLast(((ArtworkPosition) p).getArtworkRow());
            }
        }
        else;
        // artworkRows is empty! This prevent to show old artworkRows while building is loading.
        // Recall this function with wit a valid loaded building to get right artworkRows

        artworkListFragment.insertRows(artworkRows);
        artworkListFragment.refreshList();

        activeSlidingFragment = SlidingFragment.LIST;
        swapFragment(R.id.fragment_list_container, artworkListFragment);
        showNameHeaderFragment(museum);
        mainActivity.setThemeColor(MainActivity.ThemeColor.RED);
        mainActivity.getFloatingActionButton().setIconDrawable(mainActivity.getResources().getDrawable(R.drawable.white_museum));
        mainActivity.setFABListener(defaultFABOnClickListener);
    }


    public final void showMuseumListFragment() {
    // TODO: Punto di accesso per museum list
        if(DbManager.museumDownloader.isDownloadStarted() == false) {
            DbManager.museumDownloader.startDownload();
            DbManager.museumDownloader.addHandler(new JSONHandler<MuseumRow>() {
                @Override
                public void onJSONDownloadFinished(MuseumRow[] result) {
                    museumListFragment.insertRows(result);
                }
            });

        }

        simulateDeselectMuseumOnMapClick();

        activeSlidingFragment = SlidingFragment.LIST;
        swapFragment(R.id.fragment_list_container, museumListFragment);
        showSeekbarHeaderFragment();


        mainActivity.setThemeColor(MainActivity.ThemeColor.ORANGE);
        mainActivity.getFloatingActionButton().setIconDrawable(mainActivity.getResources().getDrawable(R.drawable.white_museum));
        mainActivity.setFABListener(defaultFABOnClickListener);

    }


    public final void showMuseumDetailsFragment(final MuseumRow row) {
        activeSlidingFragment = SlidingFragment.DETAILS;
        //museumDescrFragment = new MuseumDescrFragment();

        showNameHeaderFragment(row);
        museumDescrFragment.setMuseumRow(row);
        swapFragment(R.id.fragment_list_container, museumDescrFragment);

        mainActivity.setThemeColor(MainActivity.ThemeColor.PURPLE);
        mainActivity.getFloatingActionButton().setIconDrawable(mainActivity.getResources().getDrawable(R.drawable.ic_directions_white_48dp));

    }

    public final void showArtworkDetailsFragment(final ArtworkRow row)
    {
        activeSlidingFragment = SlidingFragment.DETAILS;

        showNameHeaderFragment(row);
        artworkDescrFragment = new ArtworkDescrFragment();
        artworkDescrFragment.setArtworkRow(row);
        swapFragment(R.id.fragment_list_container, artworkDescrFragment);
        if(indoorMapFragmentLite!= null && indoorMapFragmentLite.getIndoorMap() != null)
           indoorMapFragmentLite.getIndoorMap().simulateArtMarkerSelection(row);
        mainActivity.setThemeColor(MainActivity.ThemeColor.RED);
        mainActivity.getFloatingActionButton().setIconDrawable(mainActivity.getResources().getDrawable(R.drawable.ic_directions_white_48dp));
//        mainActivity.setFABListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                indoorMapFragment.navigateToSelectedMarker();
//            }
//        });

        //settare il bottone per la navigazione verso l'opera

    }



    private final void showSeekbarHeaderFragment() {
        swapFragment(R.id.fragment_sliding_header_container, seekBarHeaderFragment);
    }
    private final void showNameHeaderFragment(final IArtRow row) {
        swapFragment(R.id.fragment_sliding_header_container, nameHeaderFragment);
        nameHeaderFragment.setArtRow(row);
    }



    //+++++++++++++++++++++++++METODI BEHAVIORAL+++++++++++++++++++++++++++++++//



    @Override
    public void onClickMuseumMarker(MuseumRow museumRow) {
        showMuseumDetailsFragment(museumRow);
    }

    @Override
    public void onDeselectMuseumMarker() {
        showMuseumListFragment();
    }


    public final void simulateDeselectMuseumOnMapClick() {
        Map map = mapFragment.getMap();
        if(map != null)
            map.simulateUnselectMarker();
    }

    public final void simulateMuseumOnMapClick(final MuseumRow row){
        mapFragment.getMap().simulateMuseumClick(row);
        // showNameHeaderFragment(row);
    }
    public final MuseumRow getSelectedMuseumRow() {
        return mapFragment.getMap().getSelectedMuseumRow();
    }
    public final Map getOutdoorMap() {
        if(mapFragment != null )
            return  mapFragment.getMap();
        else return null;
    }

    public final void navigateToMuseumOnBtnClick(final MuseumRow row, View v) {
        mapFragment.getMap().simulateMuseumClick(row);
        mapFragment.onClickNavigate(v);
    }



    private View.OnClickListener defaultFABOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            switch (mainActivity.getSlidingUpPanelLayout().getPanelState())
            {

                case COLLAPSED:
                    mainActivity.getSlidingUpPanelLayout().setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);

                case ANCHORED:
                    mainActivity.getSlidingUpPanelLayout().setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);

                case EXPANDED:
                    mainActivity.getSlidingUpPanelLayout().setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);

            }
        }

    };




    //  HELPER PER INTERFACCIA GRAFICA
    public void resetInitialSeekBarRadius() {
        seekBarHeaderFragment.resetInitialSeekBarRadius();
    }


    //Metodo per lo swap di fragments
    private final void swapFragment(int containerID, Fragment newFragment) {
        if (containerID != newFragment.getId()) {
            FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(containerID, newFragment);
            //transaction.addToBackStack(null);
            transaction.commitAllowingStateLoss();
        }
    }



    public static int dpToPx(int dp)
    {
        return DpHelper.dpToPx(dp, mainActivity);
    }
    public  static int spToPx(int sp) {
        return DpHelper.spToPx(sp, mainActivity);
    }
    public static int dpToSp(int dp) {
        return DpHelper.dpToSp(dp, mainActivity);
    }


    public MainFragment getActiveMainFragment() {
        return activeMainFragment;
    }

    public SlidingFragment getActiveSlidingFragment() {
        return activeSlidingFragment;
    }

    public IndoorMap getIndoorMap() {
        return indoorMapFragmentLite.getIndoorMap();
    }
}
