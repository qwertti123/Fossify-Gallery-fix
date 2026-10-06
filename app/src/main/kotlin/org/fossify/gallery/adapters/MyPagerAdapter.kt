package org.fossify.gallery.adapters

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.viewpager.widget.PagerAdapter
import org.fossify.gallery.activities.ViewPagerActivity
import org.fossify.gallery.fragments.PhotoFragment
import org.fossify.gallery.fragments.VideoFragment
import org.fossify.gallery.fragments.ViewPagerFragment
import org.fossify.gallery.helpers.MEDIUM
import org.fossify.gallery.helpers.SHOULD_INIT_FRAGMENT
import org.fossify.gallery.models.Medium
import java.util.IdentityHashMap

/**
 * Works like FragmentStatePagerAdapter, but the fragments are tracked by their Medium instead of by their position.
 * That allows removing an item from the middle of the pager with [removeMedia] without recreating the other
 * fragments, so the video that is currently playing keeps playing when a neighbouring item gets deleted.
 */
class MyPagerAdapter(val activity: ViewPagerActivity, private val fm: FragmentManager, val media: MutableList<Medium>) : PagerAdapter() {
    private val fragments = IdentityHashMap<Medium, ViewPagerFragment>()
    private var curTransaction: FragmentTransaction? = null
    private var currentPrimaryItem: Fragment? = null
    private var isExecutingFinishUpdate = false
    private var wereStaleFragmentsRemoved = false
    var shouldInitFragment = true

    override fun getCount() = media.size

    private fun createFragment(medium: Medium): ViewPagerFragment {
        val bundle = Bundle()
        bundle.putSerializable(MEDIUM, medium)
        bundle.putBoolean(SHOULD_INIT_FRAGMENT, shouldInitFragment)
        val fragment = if (medium.isVideo()) {
            VideoFragment()
        } else {
            PhotoFragment()
        }

        fragment.arguments = bundle
        return fragment
    }

    override fun startUpdate(container: ViewGroup) {
        check(container.id != View.NO_ID) { "ViewPager with adapter $this requires a view id" }

        // fragments restored by the system after the activity was recreated are not tracked, remove them
        if (!wereStaleFragmentsRemoved) {
            wereStaleFragmentsRemoved = true
            val stale = fm.fragments.filter { it is ViewPagerFragment && !fragments.containsValue(it) }
            if (stale.isNotEmpty()) {
                val transaction = fm.beginTransaction()
                stale.forEach { transaction.remove(it) }
                transaction.commitNowAllowingStateLoss()
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val medium = media[position]
        fragments[medium]?.let { return it }

        val transaction = curTransaction ?: fm.beginTransaction().also { curTransaction = it }
        val fragment = createFragment(medium)
        fragment.listener = activity
        fragment.setMenuVisibility(false)
        fragment.userVisibleHint = false
        fragments[medium] = fragment
        transaction.add(container.id, fragment)
        return fragment
    }

    override fun destroyItem(container: ViewGroup, position: Int, any: Any) {
        val fragment = any as ViewPagerFragment
        val transaction = curTransaction ?: fm.beginTransaction().also { curTransaction = it }
        fragments.values.remove(fragment)
        if (fragment === currentPrimaryItem) {
            currentPrimaryItem = null
        }
        transaction.remove(fragment)
    }

    @Suppress("DEPRECATION")
    override fun setPrimaryItem(container: ViewGroup, position: Int, any: Any) {
        val fragment = any as Fragment
        if (fragment !== currentPrimaryItem) {
            currentPrimaryItem?.setMenuVisibility(false)
            currentPrimaryItem?.userVisibleHint = false
            fragment.setMenuVisibility(true)
            fragment.userVisibleHint = true
            currentPrimaryItem = fragment
        }
    }

    override fun finishUpdate(container: ViewGroup) {
        val transaction = curTransaction ?: return
        if (!isExecutingFinishUpdate) {
            isExecutingFinishUpdate = true
            try {
                transaction.commitNowAllowingStateLoss()
            } finally {
                isExecutingFinishUpdate = false
                curTransaction = null
            }
        }
    }

    override fun isViewFromObject(view: View, any: Any) = (any as Fragment).view === view

    override fun getItemPosition(item: Any): Int {
        for ((medium, fragment) in fragments) {
            if (fragment === item) {
                val index = media.indexOfFirst { it === medium }
                return if (index >= 0) index else PagerAdapter.POSITION_NONE
            }
        }
        return PagerAdapter.POSITION_NONE
    }

    /**
     * Removes the items with the given paths. Only the fragments of the removed items are destroyed, all the others
     * are moved to their new positions as they are, so nothing is recreated and a playing video is not interrupted.
     */
    fun removeMedia(paths: Collection<String>) {
        if (media.removeAll { paths.contains(it.path) }) {
            notifyDataSetChanged()
        }
    }

    fun getCurrentFragment(position: Int): ViewPagerFragment? {
        val medium = media.getOrNull(position) ?: return null
        return fragments[medium]
    }

    fun toggleFullscreen(isFullscreen: Boolean) {
        for (fragment in fragments.values.toList()) {
            fragment.fullscreenToggled(isFullscreen)
        }
    }
}
