package com.axp.components.gui;

import java.util.HashMap;

import com.axp.components.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;

public class DragListView extends ListView {
	private static final int STATUS_NONE = 0;
	private static final int STATUS_DRAG = 1;

	private int grabberId = -1;
	private int status = STATUS_NONE;
	private ImageView dragView = null;
	private View dragItem = null;
	//private int dragItem_height;
	private Point dragPoint = null;
	private int drag_from;
	private int drag_to;
	//private Map<Integer, Integer> items_top_paddings = new HashMap<Integer, Integer>();
	//private Map<Integer, Integer> items_heights = new HashMap<Integer, Integer>();

	WindowManager.LayoutParams lp;
	WindowManager wm;

	private MoveListener moveL = null;
	
	DragListAdapter move_adapter;

	public void setOnMoveListener(MoveListener l) {
		moveL = l;
	}

	public DragListView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DragListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		if (attrs != null) {
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.DragListView);
			grabberId = a.getResourceId(R.styleable.DragListView_grabber, -1);
			a.recycle();
		}

		lp = new WindowManager.LayoutParams();
		lp.gravity = Gravity.TOP | Gravity.LEFT;
		lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
		lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
		lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
		lp.format = PixelFormat.TRANSLUCENT;
		lp.windowAnimations = 0;

		wm = (WindowManager) getContext().getSystemService("window");
	}

	private boolean isDragListItem(View v) {
		if (v == null) return false;
		return v.findViewById(grabberId) == null ? false : true;
	}

	/*private void saveItems() {
		View v;
		int offset_pos = getFirstVisiblePosition() - getHeaderViewsCount();
		for (int i = offset_pos; (v = getChildAt(i)) != null; i++) {
			items_top_paddings.put(i, v.getPaddingTop());
			items_heights.put(i, v.getMeasuredHeight());
		}
	}*/

	/*private void restoreItems() {
		View v;
		int offset_pos = getFirstVisiblePosition() - getHeaderViewsCount();
		for (int i = offset_pos; (v = getChildAt(i)) != null; i++) {
			if (!items_top_paddings.containsKey(i)) continue;
			int top = items_top_paddings.get(i);
			v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
			v.setVisibility(View.VISIBLE);
			if (!items_heights.containsKey(i)) continue;
			ViewGroup.LayoutParams params = v.getLayoutParams();
			params.height = items_heights.get(i);
			v.setLayoutParams(params);
		}
	}*/

	/*private void moveListRepaint(int pos) {
		View v;
		int offset_pos = getFirstVisiblePosition() - getHeaderViewsCount();
		for (int i = offset_pos; (v = getChildAt(i)) != null; i++) {
			ViewGroup.LayoutParams params = v.getLayoutParams();
			if (v.equals(dragItem)) {
				if (i == pos) {
					v.setVisibility(View.INVISIBLE);
					params.height = dragItem_height;
				} else {
					params.height = 1;
				}
			} else {
				int top = items_top_paddings.containsKey(i) ? items_top_paddings.get(i) : 0;
				int height = items_heights.containsKey(i) ? items_heights.get(i) : params.height;
				if (i == pos && drag_to != pos) {
					top += dragItem_height;
					params.height = height + dragItem_height;
					v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
				} else if (drag_to != pos) {
					params.height = height;
					v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
				}
			}
			v.setLayoutParams(params);
		}
	}*/

	private boolean processTouch(MotionEvent ev) {
		int x = (int) ev.getX();
		int y = (int) ev.getY();
		int itemId = pointToPosition(x, y);
		int offset_pos = getFirstVisiblePosition() - getHeaderViewsCount();
		View item = (View) getChildAt(itemId - offset_pos);

		switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (itemId == AdapterView.INVALID_POSITION) return false;
				if (!isDragListItem(item)) return false;
				
				View dragger = item.findViewById(grabberId);

				Rect dr = new Rect();
				dragger.getDrawingRect(dr);
				int elem_y = y - item.getTop();
				int elem_x = x - item.getLeft();
				dragPoint = new Point(elem_x, elem_y);

				if (elem_x > dr.right || elem_x < dr.left || elem_y < dr.top || elem_y > dr.bottom) return false;

				//saveItems();

				item.setDrawingCacheEnabled(true);
				Bitmap cache = Bitmap.createBitmap(item.getDrawingCache());
				item.setDrawingCacheEnabled(false);

				dragView = new ImageView(getContext());
				dragView.setImageBitmap(cache);

				dragItem = item;
				//dragItem_height = item.getHeight();

				lp.x = (int) ev.getRawX() - dragPoint.x;
				lp.y = (int) ev.getRawY() - dragPoint.y;

				wm.addView(dragView, lp);

				status = STATUS_DRAG;
				drag_from = itemId;
				drag_to = drag_from;
				
				int _first = getFirstVisiblePosition();
				View _item = getChildAt(_first);
				int _top = (_item instanceof View) ? _item.getTop() : 0;
				
				move_adapter = new DragListAdapter(getAdapter());
				setAdapter(move_adapter);
				
				//setSelection(_first);
				setSelectionFromTop(_first, 0);//_top);

				//item = (View) getChildAt(itemId - offset_pos);
				//item.setVisibility(View.INVISIBLE);
				
				return true;
			case MotionEvent.ACTION_MOVE:
				if (status != STATUS_DRAG) return false;
				lp.x = (int) ev.getRawX() - dragPoint.x;
				lp.y = (int) ev.getRawY() - dragPoint.y;
				wm.updateViewLayout(dragView, lp);

				if (itemId != AdapterView.INVALID_POSITION && isDragListItem(item)) {
					//moveListRepaint(itemId);
					if (drag_to != itemId) {
						move_adapter.move(drag_to, itemId);
					}
					drag_to = itemId;
				}
				return true;
			case MotionEvent.ACTION_UP:
				if (status != STATUS_DRAG) return false;
				wm.removeView(dragView);
				dragItem.setVisibility(View.VISIBLE);
				//ViewGroup.LayoutParams params = dragItem.getLayoutParams();
				//params.height = dragItem_height;
				//dragItem.setLayoutParams(params);
				dragView.setImageDrawable(null);
				dragItem = null;
				dragView = null;
				dragPoint = null;
				
				//restoreItems();
				
				int __first = getFirstVisiblePosition();
				View __item = getChildAt(__first);
				int __top = (__item instanceof View) ? __item.getTop() : 0;
				//__top -= getFirstVisiblePoition();
				
				setAdapter(move_adapter.getOriginAdapter());
				move_adapter = null;
				
				setSelectionFromTop(__first, 0);//__top);

				if (itemId != AdapterView.INVALID_POSITION && isDragListItem(item)) drag_to = itemId;
				if (drag_to != drag_from && moveL != null) moveL.move(drag_from, drag_to);

				status = STATUS_NONE;
				return true;
		}
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (processTouch(ev)) return true;
		return super.onTouchEvent(ev);
	}

	public interface MoveListener {
		void move(int from, int to);
	}
	
	private class DragListAdapter extends BaseAdapter {
		private ListAdapter adapter;
		private HashMap<Integer, Integer> move_map = new HashMap<Integer, Integer>();
		private int[] map;
		
		public DragListAdapter(ListAdapter origin_adapter) {
			adapter = origin_adapter;
			for (int i=0; i<getCount(); i++) {
				move_map.put(i, i);
			}
			final int cnt = getCount();
			map = new int[cnt];
			for (int i=0; i<cnt; i++) {
				map[i] = i;
			}
		}
		
		public void move(int from, int to) {
			if (from == to) return;
			if (from >= getCount()) return;
			if (to < 0) return;
			
			int tmp = map[to];
			map[to] = map[from];
			if (from > to) {
				for (int i=to+1; i<=from; i++) {
					map[i] = tmp;
					if (i < from) tmp = map[i+1];
				}
			} else {
				for (int i=to-1; i>=from; i--) {
					map[i] = tmp;
					if (i > from) tmp = map[i-1];
				}
			}
			
			notifyDataSetChanged();
		}
		
		private int getMovedId(int org_id) {
			if (org_id < 0 || org_id >= getCount()) return org_id;
			return map[org_id];
		}
		
		public ListAdapter getOriginAdapter() {
			return adapter;
		}

		@Override
		public int getCount() {
			return adapter.getCount();
		}

		@Override
		public Object getItem(int pos) {
			return adapter.getItem(getMovedId(pos));
		}

		@Override
		public long getItemId(int pos) {
			return adapter.getItemId(getMovedId(pos));
		}

		int cached_visiblity = -1;
		@Override
		public View getView(int pos, View convertView, ViewGroup parent) {
			int conv_pos = getMovedId(pos);
			View v = adapter.getView(conv_pos, convertView, parent);
			if (conv_pos == drag_from) {
				cached_visiblity = v.getVisibility();
				v.setVisibility(View.INVISIBLE);
			} else if (cached_visiblity != -1) {
				v.setVisibility(cached_visiblity);
			}
			return v;
		}
	}
}
