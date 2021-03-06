import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JComboBox;
import java.util.*;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import java.lang.*;



public class CCacheSim extends JFrame implements ActionListener{

	private JPanel panelTop, panelLeft, panelRight, panelBottom;
	private JButton execStepBtn, execAllBtn, fileBotton;
	private JComboBox csBox, bsBox, wayBox, replaceBox, prefetchBox, writeBox, allocBox;
	private JFileChooser fileChoose;
	
	private JLabel labelTop,labelLeft,rightLabel,bottomLabel,fileLabel,fileAddrBtn, stepLabel1, stepLabel2,
		    csLabel, bsLabel, wayLabel, replaceLabel, prefetchLabel, writeLabel, allocLabel;
	private JLabel results[];


    //参数定义
	private String cachesize[] = { "2KB", "8KB", "32KB", "128KB", "512KB", "2MB" };
	private String blocksize[] = { "16B", "32B", "64B", "128B", "256B" };
	private String way[] = { "直接映象", "2路", "4路", "8路", "16路", "32路" };
	private String replace[] = { "LRU", "FIFO", "RAND" };
	private String pref[] = { "不预取", "不命中预取" };
	private String write[] = { "写回法", "写直达法" };
	private String alloc[] = { "按写分配", "不按写分配" };
	private String typename[] = { "读数据", "写数据", "读指令" };
	private String hitname[] = {"不命中", "命中" };
	public static final int INVALID=-1;
	
	//右侧结果显示
	private String rightLable[]={"访问总次数：","读指令次数：","读数据次数：","写数据次数："};
	
	//打开文件
	private File file;
	
	//分别表示左侧几个下拉框所选择的第几项，索引从 0 开始
	private int csIndex, bsIndex, wayIndex, replaceIndex, prefetchIndex, writeIndex, allocIndex;
	
	//其它变量定义
	//...
	
	/*
	 * 构造函数，绘制模拟器面板
	 */
	public CCacheSim(){
		super("Cache Simulator");
		draw();
	}
	
	// block
	private class CacheBlock 
	{
        int tag;
        boolean dirty;
        int LRUcount;
 
		public CacheBlock(int tag) 
		{
			this.tag = tag;
			this.LRUcount=0;
            dirty = false;
        }
    }

	// Cache
	private class Cache
	{
		private CacheBlock cache[][];
		private int     CacheSize; 
        private int     BlockSize; 
        private int     BlockNum;
        private int     BlockOffset;
        private int     BlockNumInAGroup;
        private int     GroupNum;
		private int     GroupOffset;
		private int		ReadDataMissTime, readInstMissTime, readInstHitTime, readDataHitTime;
    	private int 	writeDataHitTime, writeDataMissTime;
    	private int 	memoryWriteTime;
		private LinkedList<Integer> FIFOList = new LinkedList<Integer>();

		public int loc(int group_num, int block_num)
		{
			return group_num*BlockSize+block_num;
		}


		public Cache(int cache_size, int blk_size) 
		{
            CacheSize = cache_size;
            BlockSize = blk_size;
            BlockNum = CacheSize / BlockSize;
            BlockOffset = log2(BlockSize);
            BlockNumInAGroup = pow2(wayIndex);
            GroupNum = BlockNum / BlockNumInAGroup;
            GroupOffset = log2(GroupNum);
 
            cache = new CacheBlock[GroupNum][BlockNumInAGroup];
 
			for (int i = 0; i < GroupNum; i++) 
			{
				for (int j = 0; j < BlockNumInAGroup; j++) 
				{
                    cache[i][j] = new CacheBlock(INVALID);
                }
			}
		}

		// read: hit or not
		public boolean read(int tag, int index) 
		{
			for (int i = 0; i < BlockNumInAGroup; i++) 
			{
				if (cache[index][i].tag == tag) 
				{
					//hit
                    cache[index][i].LRUcount = -1; 
                    for(int j = 0; j < BlockNumInAGroup; j++ )
                    	cache[index][j].LRUcount ++;
                    return true;
                }
            }
            return false;
		}

		// write: hit or not
		public boolean write(int tag, int index) 
		{
			for (int i = 0; i < BlockNumInAGroup; i++) 
			{
				if (cache[index][i].tag == tag) 
				{
					//hit                  
                    cache[index][i].LRUcount = -1;
                    for(int j = 0; j < BlockNumInAGroup ;  j++ )
                    	cache[index][j].LRUcount ++;
					cache[index][i].dirty = true;
					
					if (writeIndex == 0) 
					{
						//if write back
					} 
					else if (writeIndex == 1) 
					{
						//write through
                        memoryWriteTime++;
                        cache[index][i].dirty = false;
                    }
                    return true;
                }
            }
            return false;
		}
		

		private void Replace(int tag, int index, int offset) 
		{
			if (writeIndex == 0 && cache[index][offset].dirty) 
			{
                // write back when be replaced;
                memoryWriteTime++;
            }
 
            cache[index][offset].tag = tag;
            cache[index][offset].LRUcount = 0;
            cache[index][offset].dirty = false;
			// FIFO
			FIFOList.addLast(loc(index,offset));
        }

		// replace a block in a group
		public void BlockReplace(int tag, int index) 
		{
			int Victim = 0;
			if (replaceIndex == 0)
			{
				//LRU
				for (int i = 1; i < BlockNumInAGroup; i++) 
				{
					if (cache[index][Victim].LRUcount < cache[index][i].LRUcount) 
					{
                        Victim = i;
                    }
                }
                Replace(tag, index, Victim);
			} 
			
			else if (replaceIndex == 1) 
			{
				//FIFO
				Victim=FIFOList.removeFirst().intValue();
                Replace(tag, index, Victim);
			} 
			
			else if (replaceIndex == 2) 
			{
				//random
                Victim = rand_int(0, BlockNumInAGroup);
                Replace(tag, index, Victim);
            }
        }	// end BlockReplace
		




	}	// end Cache class


	// auxiliary functions

	private int rand_int(int x, int y) 
	{
        return (int)Math.random() * (y - x) + x;
    }

	private int log2(int x) 
	{
        return (int)Math.round((Math.log(x) / Math.log(2)));
	}
	
	private int pow2(int x) 
	{
        return (int)Math.round(Math.pow(2, x));
    }


	//响应事件，共有三种事件：
	//   1. 执行到底事件
	//   2. 单步执行事件
	//   3. 文件选择事件
	public void actionPerformed(ActionEvent e){
				
		if (e.getSource() == execAllBtn) {
			simExecAll();
		}
		if (e.getSource() == execStepBtn) {
			simExecStep();
		}
		if (e.getSource() == fileBotton){
			int fileOver = fileChoose.showOpenDialog(null);
			if (fileOver == 0) {
				   String path = fileChoose.getSelectedFile().getAbsolutePath();
				   fileAddrBtn.setText(path);
				   file = new File(path);
				   readFile();
				   initCache();
			}
		}
	}
	
	/*
	 * 初始化 Cache 模拟器
	 */
	public void initCache() 
	{
		
	}
	
	/*
	 * 将指令和数据流从文件中读入
	 */
	public void readFile() {

	}
	
	/*
	 * 模拟单步执行
	 */
	public void simExecStep() {

	}
	
	/*
	 * 模拟执行到底
	 */
	public void simExecAll() {

	}

	
	public static void main(String[] args) {
		new CCacheSim();
	}
	
	/**
	 * 绘制 Cache 模拟器图形化界面
	 * 无需做修改
	 */
	public void draw() {
		//模拟器绘制面板
		setLayout(new BorderLayout(5,5));
		panelTop = new JPanel();
		panelLeft = new JPanel();
		panelRight = new JPanel();
		panelBottom = new JPanel();
		panelTop.setPreferredSize(new Dimension(800, 50));
		panelLeft.setPreferredSize(new Dimension(300, 450));
		panelRight.setPreferredSize(new Dimension(500, 450));
		panelBottom.setPreferredSize(new Dimension(800, 100));
		panelTop.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		panelLeft.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		panelRight.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		panelBottom.setBorder(new EtchedBorder(EtchedBorder.RAISED));

		//*****************************顶部面板绘制*****************************************//
		labelTop = new JLabel("Cache Simulator");
		labelTop.setAlignmentX(CENTER_ALIGNMENT);
		panelTop.add(labelTop);

		
		//*****************************左侧面板绘制*****************************************//
		labelLeft = new JLabel("Cache 参数设置");
		labelLeft.setPreferredSize(new Dimension(300, 40));
		
		//cache 大小设置
		csLabel = new JLabel("总大小");
		csLabel.setPreferredSize(new Dimension(120, 30));
		csBox = new JComboBox(cachesize);
		csBox.setPreferredSize(new Dimension(160, 30));
		csBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				csIndex = csBox.getSelectedIndex();
			}
		});
		
		//cache 块大小设置
		bsLabel = new JLabel("块大小");
		bsLabel.setPreferredSize(new Dimension(120, 30));
		bsBox = new JComboBox(blocksize);
		bsBox.setPreferredSize(new Dimension(160, 30));
		bsBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				bsIndex = bsBox.getSelectedIndex();
			}
		});
		
		//相连度设置
		wayLabel = new JLabel("相联度");
		wayLabel.setPreferredSize(new Dimension(120, 30));
		wayBox = new JComboBox(way);
		wayBox.setPreferredSize(new Dimension(160, 30));
		wayBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				wayIndex = wayBox.getSelectedIndex();
			}
		});
		
		//替换策略设置
		replaceLabel = new JLabel("替换策略");
		replaceLabel.setPreferredSize(new Dimension(120, 30));
		replaceBox = new JComboBox(replace);
		replaceBox.setPreferredSize(new Dimension(160, 30));
		replaceBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				replaceIndex = replaceBox.getSelectedIndex();
			}
		});
		
		//欲取策略设置
		prefetchLabel = new JLabel("预取策略");
		prefetchLabel.setPreferredSize(new Dimension(120, 30));
		prefetchBox = new JComboBox(pref);
		prefetchBox.setPreferredSize(new Dimension(160, 30));
		prefetchBox.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				prefetchIndex = prefetchBox.getSelectedIndex();
			}
		});
		
		//写策略设置
		writeLabel = new JLabel("写策略");
		writeLabel.setPreferredSize(new Dimension(120, 30));
		writeBox = new JComboBox(write);
		writeBox.setPreferredSize(new Dimension(160, 30));
		writeBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				writeIndex = writeBox.getSelectedIndex();
			}
		});
		
		//调块策略
		allocLabel = new JLabel("写不命中调块策略");
		allocLabel.setPreferredSize(new Dimension(120, 30));
		allocBox = new JComboBox(alloc);
		allocBox.setPreferredSize(new Dimension(160, 30));
		allocBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				allocIndex = allocBox.getSelectedIndex();
			}
		});
		
		//选择指令流文件
		fileLabel = new JLabel("选择指令流文件");
		fileLabel.setPreferredSize(new Dimension(120, 30));
		fileAddrBtn = new JLabel();
		fileAddrBtn.setPreferredSize(new Dimension(210,30));
		fileAddrBtn.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		fileBotton = new JButton("浏览");
		fileBotton.setPreferredSize(new Dimension(70,30));
		fileBotton.addActionListener(this);
		
		panelLeft.add(labelLeft);
		panelLeft.add(csLabel);
		panelLeft.add(csBox);
		panelLeft.add(bsLabel);
		panelLeft.add(bsBox);
		panelLeft.add(wayLabel);
		panelLeft.add(wayBox);
		panelLeft.add(replaceLabel);
		panelLeft.add(replaceBox);
		panelLeft.add(prefetchLabel);
		panelLeft.add(prefetchBox);
		panelLeft.add(writeLabel);
		panelLeft.add(writeBox);
		panelLeft.add(allocLabel);
		panelLeft.add(allocBox);
		panelLeft.add(fileLabel);
		panelLeft.add(fileAddrBtn);
		panelLeft.add(fileBotton);
		
		//*****************************右侧面板绘制*****************************************//
		//模拟结果展示区域
		rightLabel = new JLabel("模拟结果");
		rightLabel.setPreferredSize(new Dimension(500, 40));
		results = new JLabel[4];
		for (int i=0; i<4; i++) {
			results[i] = new JLabel("");
			results[i].setPreferredSize(new Dimension(500, 40));
		}
		
		stepLabel1 = new JLabel();
		stepLabel1.setVisible(false);
		stepLabel1.setPreferredSize(new Dimension(500, 40));
		stepLabel2 = new JLabel();
		stepLabel2.setVisible(false);
		stepLabel2.setPreferredSize(new Dimension(500, 40));
		
		panelRight.add(rightLabel);
		for (int i=0; i<4; i++) {
			panelRight.add(results[i]);
		}
		
		panelRight.add(stepLabel1);
		panelRight.add(stepLabel2);


		//*****************************底部面板绘制*****************************************//
		
		bottomLabel = new JLabel("执行控制");
		bottomLabel.setPreferredSize(new Dimension(800, 30));
		execStepBtn = new JButton("步进");
		execStepBtn.setLocation(100, 30);
		execStepBtn.addActionListener(this);
		execAllBtn = new JButton("执行到底");
		execAllBtn.setLocation(300, 30);
		execAllBtn.addActionListener(this);
		
		panelBottom.add(bottomLabel);
		panelBottom.add(execStepBtn);
		panelBottom.add(execAllBtn);

		add("North", panelTop);
		add("West", panelLeft);
		add("Center", panelRight);
		add("South", panelBottom);
		setSize(820, 620);
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
