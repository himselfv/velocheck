package asdbsd.velocheck;

/*
Fragments in ViewPager want notifications when they are shown and hidden.
ViewPager owner has to implement this manually.
*/
public interface FragmentLifecycle {
    public void onShowFragment();
    public void onHideFragment();
}
