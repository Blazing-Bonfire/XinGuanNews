package com.example.xinguannews;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.xinguannews.article.ArticleThread;
import com.example.xinguannews.ui.main.CardListFragment;
import com.example.xinguannews.ui.main.MainFragmentAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;


public class MainActivity extends AppCompatActivity
        implements SwipeRefreshLayout.OnRefreshListener, ArticleThreadListener, CategoryChipListener {
    private static boolean initted = false;

    private View linearLayoutMain;
    private Toolbar toolbar;

    private ImageButton buttonEditCategory;
    private MainFragmentAdapter cardListPagerAdapter;

    private TabLayout tabLayout;
    private ViewPager viewPager;

    // types
    private final String categoryAll = "all";
    private final String categoryNews = "news";
    private final String categoryEvent = "event";
    private final String categoryPaper = "paper";

    private LinkedHashSet<String> categories = new LinkedHashSet<>();
    private LinkedHashSet<String> selectedCategories;

    // type titles（每个类型对应的中文标题）
    private final String categoryTitleAll = "所有文章";
    private final String categoryTitleNews = "新闻";
    private final String categoryTitlePaper = "学术论文";
    private final String categoryTitleEvent = "事件";
    private final String toolbarTitle = "新冠疫情新闻";
    private final Map<String, String> mapCategoryToTitle = new HashMap<>();

    // Shared Preferences key
    private final String sharedPrefKeySelectedCategories = "Selected Categories";

    // related to edit category UI
    private BottomSheetBehavior sheetBehavior;
    private LinearLayout bottomSheetEditCategory;
    private ImageButton buttonCancelEditCategory;
    private ChipGroup chipGroupSelectedCategory;
    private ChipGroup chipGroupDeselectedCategory;
    LinkedHashMap<String, Chip> categoryChips;
    LinkedHashMap<String, Chip> selectedCategoryChips;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);        // get instance of the widget
        setSupportActionBar(toolbar);
        linearLayoutMain = findViewById(R.id.linear_layout_main);

        init();
    }

    // init methods（同时获得 XML 元素）
    private void init() {
        // 初始化
        // 注：顺序不要变

        // load from SharedPreferences
        System.out.println("loading from " + sharedPrefKeySelectedCategories);
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        Set<String> savedSelectedCategories = sharedPref.getStringSet(sharedPrefKeySelectedCategories, null);
        System.out.println(savedSelectedCategories);
        if (savedSelectedCategories != null) {
            selectedCategories = new LinkedHashSet<>();
            for (String c : savedSelectedCategories) {
                selectedCategories.add(c);
            }
        }

        initCategories();
        initEditCategoryUi();
        initTabLayout();
        initBottomNavMenu();
        initted = true;
    }

    private void initTabLayout() {
        // Setup tab layout
        cardListPagerAdapter = new MainFragmentAdapter(this, getSupportFragmentManager());

        for (String category : selectedCategories) {
            addTab(category);
        }

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        viewPager.setOffscreenPageLimit(5);
        viewPager.setAdapter(cardListPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void initBottomNavMenu() {
        BottomNavigationView bottomNavigationMenu = findViewById(R.id.nav_bottom_menu);
    }

    // 初始化所有类型的标题（中文名称），以及相关成员变量
    private void initCategories() {
        // set type titles
        mapCategoryToTitle.put(categoryAll, categoryTitleAll);
        mapCategoryToTitle.put(categoryNews, categoryTitleNews);
        mapCategoryToTitle.put(categoryPaper, categoryTitlePaper);
        mapCategoryToTitle.put(categoryEvent, categoryTitleEvent);
        categories.add(categoryAll);
        categories.add(categoryNews);
        categories.add(categoryPaper);
        categories.add(categoryEvent);
        if (selectedCategories == null) { // 不要重复初始化
            // 默认有两类：所有文章，新闻
            selectedCategories = new LinkedHashSet<String>(); // 首次赋值
            selectedCategories.add(categoryAll);
            selectedCategories.add(categoryNews);
        }
    }

    private void initEditCategoryUi() {
        // get XML elements
        buttonCancelEditCategory = findViewById(R.id.button_cancel_edit_category);
        buttonEditCategory = findViewById(R.id.button_edit_category);
        bottomSheetEditCategory = findViewById(R.id.bottom_sheet_edit_category);
        chipGroupSelectedCategory = findViewById(R.id.chip_group_selected_category);
        chipGroupDeselectedCategory = findViewById(R.id.chip_group_deselected_category);

        sheetBehavior = BottomSheetBehavior.from(bottomSheetEditCategory);

        initEditCategoryUiListeners();
        genCategoryChips();
    }

    private void initEditCategoryUiListeners() {
        // 点击修改类型
        buttonEditCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                    showEditTagSheet();
                } else {
                    hideEditTagSheet();
                }
            }
        });
        // 退出修改类型
        buttonCancelEditCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideEditTagSheet();
            }
        });

        // 针对 BottomSheet 状态发生变化的监听器
        // 自定子类以将目前的 Activity 作为参数串给监听器
        class CategoryChipBottomSheetCallback extends BottomSheetBehavior.BottomSheetCallback {
            CategoryChipListener listener;
            public CategoryChipBottomSheetCallback(CategoryChipListener listener) {
                this.listener = listener;
            }

            // 状态发生变化
            @Override
            public void onStateChanged(View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    listener.onConfirmCategorySelection();
                }
            }

            // 用户划了屏幕
            @Override
            public void onSlide(View bottomSheet, float slideOffset) {

            }
        }
        // 连接监听器
        sheetBehavior.setBottomSheetCallback(new CategoryChipBottomSheetCallback(this));
    }

    @Override
    public void onClickCategoryChip(String category) {
        System.out.println("onClickCategoryChip");
        Chip chip = categoryChips.get(category);
        if (selectedCategoryChips.containsValue(chip)) {
            selectedCategoryChips.remove(category);
            chipGroupSelectedCategory.removeView(chip); // NOTE: remove before adding
            chipGroupDeselectedCategory.addView(chip);
        } else {
            selectedCategoryChips.put(category, chip);
            chipGroupDeselectedCategory.removeView(chip); // NOTE: remove before adding
            chipGroupSelectedCategory.addView(chip);
        }
    }

    @Override
    public void onCategorySelectionChanged(String category) {

    }

    @Override
    public void onConfirmCategorySelection() {
        Set<String> curSelectedCategories = selectedCategoryChips.keySet();

        // temporary set of category that have been removed and should be removed from selectedCategorys
        // need this because we can't change the set during iteration
        Set<String> toRm = new HashSet<String>();
        // handle newly removed categories
        for (String category : selectedCategories) {
            if (!curSelectedCategories.contains(category)) {
                System.out.println(category);
                cardListPagerAdapter.removeFragmentByType(category);
                toRm.add(category);
            }
        }
        for (String s : toRm) selectedCategories.remove(s);

        // handle newly added categories
        for (String category : curSelectedCategories) {
            if (!selectedCategories.contains(category)) {
                System.out.println(category);
                // Added new category => add new tab
                addTab(category);
//                cardListPagerAdapter.getFragmentByType(category).onRefresh();
                selectedCategories.add(category);
            }
        }

        // save selected categories
        System.out.println(selectedCategories);
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putStringSet(sharedPrefKeySelectedCategories, selectedCategories);
        editor.commit();
    }

    // 根据此类的成员变量 categories, 生成相应的 CategoryChip，并添加到相应 ChipGroup 里。
    // 同时，对其添加一个 onClick 监听器
    private void genCategoryChips() {
        class CategoryChipOnClickListener implements View.OnClickListener {
            private String category;
            private CategoryChipListener listener;
            public CategoryChipOnClickListener(String category, CategoryChipListener listener) {
                this.category = category;
                this.listener = listener;
            }

            @Override
            public void onClick(View view) {
                listener.onClickCategoryChip(category);
            }
        }
        categoryChips = new LinkedHashMap<>();
        selectedCategoryChips = new LinkedHashMap<>();
        for (String category : categories) {
            Chip chip = (Chip) this.getLayoutInflater().inflate(R.layout.layout_category_chip, null, false);
            chip.setText(mapCategoryToTitle.get(category));
            categoryChips.put(category, chip);
            if (selectedCategories.contains(category)) {
                chipGroupSelectedCategory.addView(chip);
                selectedCategoryChips.put(category, chip);
            } else {
                chipGroupDeselectedCategory.addView(chip);
            }

            // set onClickListener
            chip.setOnClickListener(new CategoryChipOnClickListener(category, this));
        }
    }

    private void setCategoryChips(List<String> categorys) {
        for (String category : categorys) {
            Chip chip = (Chip) this.getLayoutInflater().inflate(R.layout.layout_category_chip, null, false);
            chip.setText(category);

        }
    }

    private void showEditTagSheet() {
        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        darken();
    }

    private void hideEditTagSheet() {
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        undarken();
    }

    private void darken() {
//        darkener.setVisibility(View.VISIBLE);
        linearLayoutMain.setClickable(false);
    }

    private void undarken() {
        linearLayoutMain.setClickable(true);
    }

    private void addTab(String category) {
        String title = mapCategoryToTitle.get(category);
        cardListPagerAdapter.addFragment(new CardListFragment(category, title));
    }

    @Override
    public void onFinishGettingArticles(ArticleThread thread) {
        for (CardListFragment fragment : cardListPagerAdapter.getFragments()) {
            fragment.onFinishGettingArticles(thread);
        }
    }

    @Override
    public void onRefresh() {
        for (CardListFragment fragment : cardListPagerAdapter.getFragments()) {
            fragment.onRefresh();
        }
    }
}



