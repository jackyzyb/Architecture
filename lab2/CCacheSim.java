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


    //��������
	private String cachesize[] = { "2KB", "8KB", "32KB", "128KB", "512KB", "2MB" };
	private String blocksize[] = { "16B", "32B", "64B", "128B", "256B" };
	private String way[] = { "ֱ��ӳ��", "2·", "4·", "8·", "16·", "32·" };
	private String replace[] = { "LRU", "FIFO", "RAND" };
	private String pref[] = { "��Ԥȡ", "������Ԥȡ" };
	private String write[] = { "д�ط�", "дֱ�﷨" };
	private String alloc[] = { "��д����", "����д����" };
	private String typename[] = { "������", "д����", "��ָ��" };
	private String hitname[] = {"������", "����" };
	public static final int INVALID=-1;
	
	//�Ҳ�����ʾ
	private String rightLable[]={"�����ܴ�����","��ָ�������","�����ݴ�����","д���ݴ�����"};
	
	//���ļ�
	private File file;
	
	//�ֱ��ʾ��༸����������ѡ��ĵڼ�������� 0 ��ʼ
	private int csIndex, bsIndex, wayIndex, replaceIndex, prefetchIndex, writeIndex, allocIndex;
	
	//������������
	//...
	
	/*
	 * ���캯��������ģ�������
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


	//��Ӧ�¼������������¼���
	//   1. ִ�е����¼�
	//   2. ����ִ���¼�
	//   3. �ļ�ѡ���¼�
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
	 * ��ʼ�� Cache ģ����
	 */
	public void initCache() 
	{
		
	}
	
	/*
	 * ��ָ������������ļ��ж���
	 */
	public void readFile() {

	}
	
	/*
	 * ģ�ⵥ��ִ��
	 */
	public void simExecStep() {

	}
	
	/*
	 * ģ��ִ�е���
	 */
	public void simExecAll() {

	}

	
	public static void main(String[] args) {
		new CCacheSim();
	}
	
	/**
	 * ���� Cache ģ����ͼ�λ�����
	 * �������޸�
	 */
	public void draw() {
		//ģ�����������
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

		//*****************************����������*****************************************//
		labelTop = new JLabel("Cache Simulator");
		labelTop.setAlignmentX(CENTER_ALIGNMENT);
		panelTop.add(labelTop);

		
		//*****************************���������*****************************************//
		labelLeft = new JLabel("Cache ��������");
		labelLeft.setPreferredSize(new Dimension(300, 40));
		
		//cache ��С����
		csLabel = new JLabel("�ܴ�С");
		csLabel.setPreferredSize(new Dimension(120, 30));
		csBox = new JComboBox(cachesize);
		csBox.setPreferredSize(new Dimension(160, 30));
		csBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				csIndex = csBox.getSelectedIndex();
			}
		});
		
		//cache ���С����
		bsLabel = new JLabel("���С");
		bsLabel.setPreferredSize(new Dimension(120, 30));
		bsBox = new JComboBox(blocksize);
		bsBox.setPreferredSize(new Dimension(160, 30));
		bsBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				bsIndex = bsBox.getSelectedIndex();
			}
		});
		
		//����������
		wayLabel = new JLabel("������");
		wayLabel.setPreferredSize(new Dimension(120, 30));
		wayBox = new JComboBox(way);
		wayBox.setPreferredSize(new Dimension(160, 30));
		wayBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				wayIndex = wayBox.getSelectedIndex();
			}
		});
		
		//�滻��������
		replaceLabel = new JLabel("�滻����");
		replaceLabel.setPreferredSize(new Dimension(120, 30));
		replaceBox = new JComboBox(replace);
		replaceBox.setPreferredSize(new Dimension(160, 30));
		replaceBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				replaceIndex = replaceBox.getSelectedIndex();
			}
		});
		
		//��ȡ��������
		prefetchLabel = new JLabel("Ԥȡ����");
		prefetchLabel.setPreferredSize(new Dimension(120, 30));
		prefetchBox = new JComboBox(pref);
		prefetchBox.setPreferredSize(new Dimension(160, 30));
		prefetchBox.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				prefetchIndex = prefetchBox.getSelectedIndex();
			}
		});
		
		//д��������
		writeLabel = new JLabel("д����");
		writeLabel.setPreferredSize(new Dimension(120, 30));
		writeBox = new JComboBox(write);
		writeBox.setPreferredSize(new Dimension(160, 30));
		writeBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				writeIndex = writeBox.getSelectedIndex();
			}
		});
		
		//�������
		allocLabel = new JLabel("д�����е������");
		allocLabel.setPreferredSize(new Dimension(120, 30));
		allocBox = new JComboBox(alloc);
		allocBox.setPreferredSize(new Dimension(160, 30));
		allocBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				allocIndex = allocBox.getSelectedIndex();
			}
		});
		
		//ѡ��ָ�����ļ�
		fileLabel = new JLabel("ѡ��ָ�����ļ�");
		fileLabel.setPreferredSize(new Dimension(120, 30));
		fileAddrBtn = new JLabel();
		fileAddrBtn.setPreferredSize(new Dimension(210,30));
		fileAddrBtn.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		fileBotton = new JButton("���");
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
		
		//*****************************�Ҳ�������*****************************************//
		//ģ����չʾ����
		rightLabel = new JLabel("ģ����");
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


		//*****************************�ײ�������*****************************************//
		
		bottomLabel = new JLabel("ִ�п���");
		bottomLabel.setPreferredSize(new Dimension(800, 30));
		execStepBtn = new JButton("����");
		execStepBtn.setLocation(100, 30);
		execStepBtn.addActionListener(this);
		execAllBtn = new JButton("ִ�е���");
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
