import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class POSSystem extends JFrame {
    private static final Color GREEN = new Color(24, 60, 53);
    private static final Color LIGHT_GREEN = new Color(225, 238, 230);
    private static final Color BG = new Color(245, 247, 246);
    private static final Color TEXT = new Color(40, 55, 49);

    private Product[] products = new Product[100];
    private User[] users = new User[50];
    private Sale[] sales = new Sale[500];
    private int productCount = 0, userCount = 0, saleCount = 0, nextSaleId = 1001;
    private TransactionStack recentTransactions = new TransactionStack(500); // same size as sales[] so both always stay in sync
    private TransactionStack removedTransactions = new TransactionStack(500); // second Stack: the removed (undone) sales
    private static final String HISTORY_FILE = "removed_history.txt";        // removed history is saved here
    private User currentUser;

    private JPanel content;
    private JLabel roleLabel;
    private DefaultTableModel productModel, transactionModel;
    private JTable productTable, transactionTable;

    public POSSystem() {
        setTitle("GreenPOS | Point of Sale System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 680);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        loadAll();
        addWindowListener(new java.awt.event.WindowAdapter(){ public void windowClosing(java.awt.event.WindowEvent e){ saveAll(); } }); // last safety save
        showLogin();
    }

    private void seedProducts() {
        addProduct("P001", "Bottled Water", 20.00, 40);
        addProduct("P002", "Potato Chips", 35.00, 25);
        addProduct("P003", "Chocolate Bar", 28.00, 30);
        addProduct("P004", "Instant Noodles", 18.00, 50);
        addProduct("P005", "Iced Tea", 25.00, 20);
    }

    private void seedUsers() {
        users[userCount++] = new Admin("admin", "admin123", "Store Administrator");
        users[userCount++] = new Employee("employee", "emp123", "Regular Employee");
    }

    private void showLogin() {
        getContentPane().removeAll();
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(BG);
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 231, 227)), new EmptyBorder(30, 36, 30, 36)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel logo = new JLabel("▣  GreenPOS");
        logo.setFont(new Font("SansSerif", Font.BOLD, 28)); logo.setForeground(GREEN); logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel subtitle = new JLabel("Retail Management System");
        subtitle.setForeground(new Color(110, 125, 117)); subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel heading = new JLabel("Welcome back"); heading.setFont(new Font("SansSerif", Font.BOLD, 22)); heading.setForeground(TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel userLabel = new JLabel("Username"); userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField username = new JTextField(); username.setMaximumSize(new Dimension(340, 38)); username.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JLabel passLabel = new JLabel("Password"); passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPasswordField password = new JPasswordField(); password.setMaximumSize(new Dimension(340, 38)); password.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JButton login = button("Sign in", GREEN, Color.WHITE); login.setAlignmentX(Component.LEFT_ALIGNMENT); login.setMaximumSize(new Dimension(340, 42));
        JLabel hint = new JLabel("Demo admin: admin / admin123"); hint.setForeground(new Color(110,125,117)); hint.setFont(new Font("SansSerif", Font.PLAIN, 12)); hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel hint2 = new JLabel("Demo employee: employee / emp123"); hint2.setForeground(new Color(110,125,117)); hint2.setFont(new Font("SansSerif", Font.PLAIN, 12)); hint2.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(logo); card.add(Box.createVerticalStrut(4)); card.add(subtitle); card.add(Box.createVerticalStrut(28)); card.add(heading);
        card.add(Box.createVerticalStrut(20)); card.add(userLabel); card.add(Box.createVerticalStrut(6)); card.add(username);
        card.add(Box.createVerticalStrut(14)); card.add(passLabel); card.add(Box.createVerticalStrut(6)); card.add(password);
        card.add(Box.createVerticalStrut(22)); card.add(login); card.add(Box.createVerticalStrut(16)); card.add(hint); card.add(Box.createVerticalStrut(4)); card.add(hint2);
        outer.add(card); add(outer);
        login.addActionListener(e -> {
            String name = username.getText().trim(); String pass = new String(password.getPassword());
            User found = findUser(name);
            if (found != null && found.login(pass)) { currentUser = found; showMain(); }
            else JOptionPane.showMessageDialog(this, "Incorrect username or password.", "Login failed", JOptionPane.WARNING_MESSAGE);
        });
        password.addActionListener(e -> login.doClick());
        revalidate(); repaint();
    }

    private void showMain() {
        getContentPane().removeAll();
        JPanel root = new JPanel(new BorderLayout()); root.setBackground(BG);
        JPanel sidebar = new JPanel(); sidebar.setBackground(new Color(232, 239, 235)); sidebar.setPreferredSize(new Dimension(190, 0));
        sidebar.setBorder(new EmptyBorder(20, 12, 16, 12)); sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        JLabel brand = new JLabel("▣  GreenPOS"); brand.setFont(new Font("SansSerif", Font.BOLD, 21)); brand.setForeground(GREEN); brand.setBorder(new EmptyBorder(0, 6, 26, 0));
        sidebar.add(brand);
        addNav(sidebar, "Dashboard", () -> showDashboard());
        addNav(sidebar, "New Sale", () -> showSalesScreen());
        if (currentUser instanceof Admin) addNav(sidebar, "Products", () -> showProducts());
        addNav(sidebar, "Transactions", () -> showTransactions());
        if (currentUser instanceof Admin) addNav(sidebar, "Employees", () -> showEmployees());
        sidebar.add(Box.createVerticalGlue());
        JButton logout = button("Log out", Color.WHITE, GREEN); logout.setAlignmentX(Component.LEFT_ALIGNMENT); logout.setMaximumSize(new Dimension(165, 38));
        logout.setBorder(BorderFactory.createLineBorder(new Color(205, 218, 210))); logout.addActionListener(e -> { currentUser = null; showLogin(); }); sidebar.add(logout);

        JPanel top = new JPanel(new BorderLayout()); top.setBackground(Color.WHITE); top.setBorder(new EmptyBorder(15, 24, 15, 24));
        JLabel page = new JLabel("Retail Management"); page.setFont(new Font("SansSerif", Font.BOLD, 18)); page.setForeground(TEXT);
        JPanel userBox = new JPanel(); userBox.setOpaque(false); userBox.setLayout(new BoxLayout(userBox, BoxLayout.Y_AXIS));
        JLabel userName = new JLabel(currentUser.getFullName()); userName.setFont(new Font("SansSerif", Font.BOLD, 13)); userName.setForeground(TEXT); userName.setAlignmentX(Component.RIGHT_ALIGNMENT);
        roleLabel = new JLabel(currentUser instanceof Admin ? "Administrator" : "Employee"); roleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12)); roleLabel.setForeground(new Color(105,120,112)); roleLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        userBox.add(userName); userBox.add(roleLabel); top.add(page, BorderLayout.WEST); top.add(userBox, BorderLayout.EAST);
        content = new JPanel(new BorderLayout()); content.setBackground(BG); content.setBorder(new EmptyBorder(24, 24, 24, 24));
        root.add(sidebar, BorderLayout.WEST); root.add(top, BorderLayout.NORTH); root.add(content, BorderLayout.CENTER);
        setContentPane(root); showDashboard(); revalidate(); repaint();
    }

    private void addNav(JPanel sidebar, String name, Runnable action) {
        JButton b = button(name, new Color(232,239,235), GREEN); b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setMaximumSize(new Dimension(170, 42)); b.setBorder(new EmptyBorder(10, 12, 10, 8)); b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.addActionListener(e -> action.run()); sidebar.add(b); sidebar.add(Box.createVerticalStrut(5));
    }

    private void showDashboard() {
        content.removeAll(); JPanel page = new JPanel(new BorderLayout(0, 18)); page.setOpaque(false);
        JPanel heading = new JPanel(); heading.setOpaque(false); heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Dashboard"); title.setFont(new Font("SansSerif", Font.BOLD, 25)); title.setForeground(TEXT);
        JLabel desc = new JLabel("Here's an overview of your store."); desc.setForeground(new Color(110,125,117));
        heading.add(title); heading.add(Box.createVerticalStrut(5)); heading.add(desc);
        JPanel cards = new JPanel(new GridLayout(1, 3, 14, 0)); cards.setOpaque(false);
        cards.add(statCard("Today's Sales", String.format("₱%.2f", getTodaySales()), "Completed transactions"));
        cards.add(statCard("Products", String.valueOf(productCount), "Products in catalog"));
        cards.add(statCard("Low Stock", String.valueOf(lowStockCount()), "Items with 5 or fewer left"));
        JPanel middle = new JPanel(new BorderLayout(0, 10)); middle.setOpaque(false);
        JLabel quickTitle = new JLabel("Quick actions"); quickTitle.setFont(new Font("SansSerif", Font.BOLD, 17)); quickTitle.setForeground(TEXT);
        JPanel quick = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12)); quick.setBackground(Color.WHITE); quick.setBorder(new EmptyBorder(8, 8, 8, 8));
        JButton sale = button("+  Start New Sale", GREEN, Color.WHITE); sale.setPreferredSize(new Dimension(160, 42)); sale.addActionListener(e -> showSalesScreen()); quick.add(sale);
        if (currentUser instanceof Admin) { JButton productsBtn = button("Manage Products", LIGHT_GREEN, GREEN); productsBtn.setPreferredSize(new Dimension(160, 42)); productsBtn.addActionListener(e -> showProducts()); quick.add(productsBtn); }
        JButton tx = button("View Transactions", LIGHT_GREEN, GREEN); tx.setPreferredSize(new Dimension(165, 42)); tx.addActionListener(e -> showTransactions()); quick.add(tx);
        middle.add(quickTitle, BorderLayout.NORTH); middle.add(quick, BorderLayout.CENTER);
        page.add(heading, BorderLayout.NORTH); page.add(cards, BorderLayout.CENTER); page.add(middle, BorderLayout.SOUTH);
        content.add(page); content.revalidate(); content.repaint();
    }

    private JPanel statCard(String label, String value, String note) {
        JPanel p = new JPanel(); p.setBackground(Color.WHITE); p.setBorder(new EmptyBorder(18, 18, 18, 18)); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(label); l.setForeground(new Color(105,120,112)); l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JLabel v = new JLabel(value); v.setForeground(GREEN); v.setFont(new Font("SansSerif", Font.BOLD, 25));
        JLabel n = new JLabel(note); n.setForeground(new Color(130,140,135)); n.setFont(new Font("SansSerif", Font.PLAIN, 11));
        p.add(l); p.add(Box.createVerticalStrut(12)); p.add(v); p.add(Box.createVerticalStrut(6)); p.add(n); return p;
    }

    private void showProducts() {
        if (!(currentUser instanceof Admin)) { deny(); return; }
        content.removeAll(); JPanel page = new JPanel(new BorderLayout(0, 15)); page.setOpaque(false);
        JLabel title = new JLabel("Product Management"); title.setFont(new Font("SansSerif", Font.BOLD, 24)); title.setForeground(TEXT);
        productModel = new DefaultTableModel(new Object[]{"ID", "Product", "Price (₱)", "Stock"}, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        productTable = new JTable(productModel); styleTable(productTable); refreshProductTable();
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); actions.setOpaque(false);
        JButton add = button("+ Add Product", GREEN, Color.WHITE); JButton edit = button("Edit Selected", LIGHT_GREEN, GREEN); JButton restock = button("Restock", LIGHT_GREEN, GREEN); JButton remove = button("Remove", new Color(250,232,230), new Color(150,55,45));
        add.addActionListener(e -> productDialog(null)); edit.addActionListener(e -> { int r=productTable.getSelectedRow(); if(r<0) warnSelect(); else productDialog(findProductById(String.valueOf(productModel.getValueAt(r,0)))); });
        restock.addActionListener(e -> restockProduct()); remove.addActionListener(e -> removeProduct());
        actions.add(add); actions.add(edit); actions.add(restock); actions.add(remove);
        page.add(title, BorderLayout.NORTH); page.add(new JScrollPane(productTable), BorderLayout.CENTER); page.add(actions, BorderLayout.SOUTH); content.add(page); content.revalidate(); content.repaint();
    }

    private void productDialog(Product p) {
        JTextField id = new JTextField(p == null ? "P" + String.format("%03d", productCount + 1) : p.id);
        JTextField name = new JTextField(p == null ? "" : p.name);
        JTextField price = new JTextField(p == null ? "" : String.valueOf(p.price));
        JTextField stock = new JTextField(p == null ? "" : String.valueOf(p.stock));
        Object[] fields = {"Product ID", id, "Product name", name, "Price", price, "Stock quantity", stock};
        if (JOptionPane.showConfirmDialog(this, fields, p == null ? "Add Product" : "Edit Product", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            String pid=id.getText().trim(), pname=name.getText().trim(); double pr=Double.parseDouble(price.getText().trim()); int qty=Integer.parseInt(stock.getText().trim());
            if(pid.isEmpty()||pname.isEmpty()||pr<0||qty<0) throw new IllegalArgumentException();
            if (p == null && findProductById(pid) != null) { JOptionPane.showMessageDialog(this,"That product ID already exists."); return; }
            if (p == null) { if(productCount==products.length) { warn("Product list is full."); return; } addProduct(pid,pname,pr,qty); }
            else { p.id=pid; p.name=pname; p.price=pr; p.stock=qty; }
            refreshProductTable(); saveAll(); showProducts();
        } catch(Exception ex) { warn("Enter a valid name, unique ID, non-negative price, and whole-number stock."); }
    }

    private void restockProduct() {
        int r=productTable.getSelectedRow(); if(r<0){warnSelect();return;} Product p=findProductById(String.valueOf(productModel.getValueAt(r,0)));
        String s=JOptionPane.showInputDialog(this,"How many units should be added?","Restock",JOptionPane.QUESTION_MESSAGE); if(s==null)return;
        try { int q=Integer.parseInt(s.trim()); if(q<=0)throw new Exception(); p.stock+=q; saveAll(); showProducts(); }
        catch(Exception ex){warn("Enter a positive whole number.");}
    }
    private void removeProduct() {
        int r=productTable.getSelectedRow(); if(r<0){warnSelect();return;} String id=String.valueOf(productModel.getValueAt(r,0));
        if(JOptionPane.showConfirmDialog(this,"Remove this product?","Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        for(int i=0;i<productCount;i++) if(products[i].id.equals(id)){ for(int j=i;j<productCount-1;j++)products[j]=products[j+1]; products[--productCount]=null; break; }
        saveAll(); showProducts();
    }
    private void refreshProductTable() { if(productModel==null)return; productModel.setRowCount(0); for(int i=0;i<productCount;i++) productModel.addRow(new Object[]{products[i].id,products[i].name,String.format("%.2f",products[i].price),products[i].stock}); }

    private void showSalesScreen() {
        content.removeAll(); JPanel page = new JPanel(new BorderLayout(0, 14)); page.setOpaque(false);
        JLabel title = new JLabel("New Sale"); title.setFont(new Font("SansSerif", Font.BOLD, 24)); title.setForeground(TEXT);
        DefaultTableModel cartModel = new DefaultTableModel(new Object[]{"ID","Product","Price","Qty","Subtotal"},0) { public boolean isCellEditable(int r,int c){return false;} };
        JTable cartTable = new JTable(cartModel); styleTable(cartTable);
        JComboBox<String> productChoice = new JComboBox<>(); for(int i=0;i<productCount;i++)productChoice.addItem(products[i].id+" - "+products[i].name+" (stock: "+products[i].stock+")");
        JTextField qtyField = new JTextField("1",5); JButton add = button("Add to Cart", LIGHT_GREEN, GREEN);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT,8,8)); controls.setBackground(Color.WHITE); controls.add(new JLabel("Product")); controls.add(productChoice); controls.add(new JLabel("Quantity")); controls.add(qtyField); controls.add(add);
        JPanel cartPanel = new JPanel(new BorderLayout(0,8)); cartPanel.setOpaque(false); cartPanel.add(controls,BorderLayout.NORTH); cartPanel.add(new JScrollPane(cartTable),BorderLayout.CENTER);
        JPanel checkout = new JPanel(); checkout.setBackground(Color.WHITE); checkout.setBorder(new EmptyBorder(16,16,16,16)); checkout.setLayout(new BoxLayout(checkout,BoxLayout.Y_AXIS));
        JLabel totalLabel = new JLabel("Total: ₱0.00"); totalLabel.setFont(new Font("SansSerif",Font.BOLD,22)); totalLabel.setForeground(GREEN);
        JTextField paymentField = new JTextField(); paymentField.setMaximumSize(new Dimension(Integer.MAX_VALUE,34)); JButton pay = button("Complete Sale", GREEN, Color.WHITE); JButton removeItem = button("Remove Selected Item", new Color(250,232,230), new Color(150,55,45));
        checkout.add(totalLabel); checkout.add(Box.createVerticalStrut(12)); checkout.add(new JLabel("Amount received (₱)")); checkout.add(Box.createVerticalStrut(5)); checkout.add(paymentField); checkout.add(Box.createVerticalStrut(12)); checkout.add(pay); checkout.add(Box.createVerticalStrut(8)); checkout.add(removeItem);
        JPanel body = new JPanel(new BorderLayout(14,0)); body.setOpaque(false); body.add(cartPanel,BorderLayout.CENTER); body.add(checkout,BorderLayout.EAST); checkout.setPreferredSize(new Dimension(225,0));
        java.util.ArrayList<CartItem> cart = new java.util.ArrayList<>();
        Runnable updateCart = () -> { cartModel.setRowCount(0); double total=0; for(CartItem ci:cart){double sub=ci.product.price*ci.quantity; total+=sub; cartModel.addRow(new Object[]{ci.product.id,ci.product.name,String.format("%.2f",ci.product.price),ci.quantity,String.format("%.2f",sub)});} totalLabel.setText(String.format("Total: ₱%.2f",total)); };
        add.addActionListener(e -> {
            if(productChoice.getSelectedIndex()<0){warn("There are no products available.");return;}
            try { int q=Integer.parseInt(qtyField.getText().trim()); if(q<=0)throw new Exception(); String selected=(String)productChoice.getSelectedItem(); String id=selected.substring(0,selected.indexOf(" - ")); Product p=findProductById(id); if(p==null) return;
                int already=0; for(CartItem ci:cart)if(ci.product==p)already+=ci.quantity;
                if(q+already>p.stock){warn("Not enough stock available.");return;}
                CartItem found=null; for(CartItem ci:cart)if(ci.product==p)found=ci; if(found==null)cart.add(new CartItem(p,q));else found.quantity+=q; updateCart.run();
            } catch(Exception ex){warn("Enter a valid positive quantity.");}
        });
        removeItem.addActionListener(e->{int r=cartTable.getSelectedRow();if(r<0){warnSelect();return;}cart.remove(r);updateCart.run();});
        pay.addActionListener(e -> {
            if(cart.isEmpty()){warn("Add at least one item to the cart.");return;}
            double total=0; for(CartItem ci:cart)total+=ci.product.price*ci.quantity;
            try { double received=Double.parseDouble(paymentField.getText().trim()); if(received<total){warn("Payment is less than the total.");return;}
                for(CartItem ci:cart)if(ci.quantity>ci.product.stock){warn("Stock changed. Please review the cart.");return;}
                for(CartItem ci:cart)ci.product.stock-=ci.quantity;
                Sale s=new Sale(nextSaleId++,currentUser.getUsername(),cart.toArray(new CartItem[0]),total,received,LocalDateTime.now());
                if(saleCount<sales.length)sales[saleCount++]=s; recentTransactions.push(s); saveAll();
                String receipt=buildReceipt(s); JTextArea area=new JTextArea(receipt,18,32); area.setEditable(false); area.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
                JOptionPane.showMessageDialog(this,new JScrollPane(area),"Sale Completed",JOptionPane.INFORMATION_MESSAGE);
                showDashboard();
            } catch(Exception ex){warn("Enter a valid payment amount.");}
        });
        page.add(title,BorderLayout.NORTH); page.add(body,BorderLayout.CENTER); content.add(page); content.revalidate(); content.repaint();
    }

    private String buildReceipt(Sale s) {
        StringBuilder b=new StringBuilder(); b.append("             GREENPOS\n"); b.append("--------------------------------\n"); b.append("Receipt #: ").append(s.id).append("\nCashier: ").append(s.cashier).append("\nDate: ").append(s.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n"); b.append("--------------------------------\n");
        for(CartItem ci:s.items)b.append(ci.product.name).append(" x").append(ci.quantity).append("  ₱").append(String.format("%.2f",ci.product.price*ci.quantity)).append("\n");
        b.append("--------------------------------\nTotal:     ₱").append(String.format("%.2f",s.total)).append("\nReceived:  ₱").append(String.format("%.2f",s.received)).append("\nChange:    ₱").append(String.format("%.2f",s.received-s.total)).append("\n\nThank you!"); return b.toString();
    }

    private void showTransactions() {
        content.removeAll(); JPanel page=new JPanel(new BorderLayout(0,14)); page.setOpaque(false);
        JLabel title=new JLabel("Transactions"); title.setFont(new Font("SansSerif",Font.BOLD,24)); title.setForeground(TEXT);
        transactionModel=new DefaultTableModel(new Object[]{"Receipt #","Date","Cashier","Total (₱)","Received (₱)","Change (₱)"},0){public boolean isCellEditable(int r,int c){return false;}};
        transactionTable=new JTable(transactionModel); styleTable(transactionTable);
        for(int i=saleCount-1;i>=0;i--){Sale s=sales[i]; transactionModel.addRow(new Object[]{s.id,s.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),s.cashier,String.format("%.2f",s.total),String.format("%.2f",s.received),String.format("%.2f",s.received-s.total)});}
        JPanel bottom=new JPanel(new GridLayout(2,1,0,6)); bottom.setOpaque(false);
        JPanel row1=new JPanel(new FlowLayout(FlowLayout.LEFT)); row1.setOpaque(false);
        JPanel row2=new JPanel(new FlowLayout(FlowLayout.LEFT)); row2.setOpaque(false); bottom.add(row1); bottom.add(row2);
        JLabel summary=new JLabel("Total recorded sales: ₱"+String.format("%.2f",getAllSales())); summary.setFont(new Font("SansSerif",Font.BOLD,15)); summary.setForeground(GREEN); row1.add(summary);
        JButton latest=button("Show Latest Stack Entry",LIGHT_GREEN,GREEN); latest.addActionListener(e->{Sale s=recentTransactions.peek();if(s==null)warn("The transaction Stack is empty.");else JOptionPane.showMessageDialog(this,buildReceipt(s),"Most Recent Transaction",JOptionPane.INFORMATION_MESSAGE);}); row2.add(latest);
        JButton removeLatest=button("Remove Latest Entry",LIGHT_GREEN,GREEN); removeLatest.addActionListener(e->removeLatestSale()); row2.add(removeLatest);
        JButton history=button("Removed History",LIGHT_GREEN,GREEN); history.addActionListener(e->showRemovedHistory()); row2.add(history);
        JButton restore=button("Restore Last Removed",LIGHT_GREEN,GREEN); restore.addActionListener(e->restoreLastRemoved()); row2.add(restore);
        page.add(title,BorderLayout.NORTH); page.add(new JScrollPane(transactionTable),BorderLayout.CENTER); page.add(bottom,BorderLayout.SOUTH); content.add(page); content.revalidate(); content.repaint();
    }

    /** Undo the most recent sale using the Stack (LIFO): pop() takes off the newest sale,
     *  then we restore the stock and remove it from the sales records. */
    private void removeLatestSale() {
        Sale s = recentTransactions.peek();                       // look first, do not remove yet
        if (s == null) { warn("The transaction Stack is empty."); return; }
        int choice = JOptionPane.showConfirmDialog(this,
            "Remove Sale #" + s.id + " (₱" + String.format("%.2f", s.total) + ") and restore its stock?",
            "Confirm Remove", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        s = recentTransactions.pop();                             // POP: newest sale leaves the Stack
        s.removedAt = LocalDateTime.now(); s.removedBy = currentUser.getUsername();
        removedTransactions.push(s);                              // PUSH it onto the removed Stack (history)
        saveAll();                                     // keep the history in a file
        for (CartItem ci : s.items) { Product inv = findProductById(ci.product.id); if (inv != null) inv.stock += ci.quantity; }  // put the sold items back in stock
        if (saleCount > 0 && sales[saleCount - 1] == s) sales[--saleCount] = null; // newest sale is always last in sales[]
        JOptionPane.showMessageDialog(this, "Sale #" + s.id + " was removed and its stock restored.",
            "Removed", JOptionPane.INFORMATION_MESSAGE);
        showTransactions();                                       // refresh the table and the total
    }

    /** Restore the most recently removed sale (LIFO again: the last one removed is the first one restored). */
    private void restoreLastRemoved() {
        Sale s = removedTransactions.peek();
        if (s == null) { warn("There are no removed sales to restore."); return; }
        if (saleCount >= sales.length) { warn("The sales list is full."); return; }
        for (CartItem ci : s.items) {                             // check first, change nothing yet
            Product inv = findProductById(ci.product.id);
            if (inv == null) { warn("Cannot restore: product \"" + ci.product.name + "\" no longer exists."); return; }
            if (inv.stock < ci.quantity) { warn("Not enough stock to restore Sale #" + s.id + " (" + inv.name + ")."); return; }
        }
        int choice = JOptionPane.showConfirmDialog(this, "Restore Sale #" + s.id + " (₱" + String.format("%.2f", s.total) + ")? Its items will be taken from stock again.",
            "Confirm Restore", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        s = removedTransactions.pop();                            // POP from the removed Stack
        for (CartItem ci : s.items) { Product inv = findProductById(ci.product.id); inv.stock -= ci.quantity; ci.product = inv; }
        s.removedAt = null; s.removedBy = null;
        sales[saleCount++] = s; recentTransactions.push(s);       // back into the sales records and the main Stack
        saveAll();
        JOptionPane.showMessageDialog(this, "Sale #" + s.id + " was restored.", "Restored", JOptionPane.INFORMATION_MESSAGE);
        showTransactions();
    }

    /** Show every removed sale, newest removal first (view only). */
    private void showRemovedHistory() {
        if (removedTransactions.isEmpty()) { JOptionPane.showMessageDialog(this, "No sales have been removed yet.", "Removed History", JOptionPane.INFORMATION_MESSAGE); return; }
        DefaultTableModel m = new DefaultTableModel(new Object[]{"Receipt #","Sold On","Cashier","Total (₱)","Removed On","Removed By"},0){public boolean isCellEditable(int r,int c){return false;}};
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (int i = removedTransactions.size() - 1; i >= 0; i--) {   // top of the Stack first
            Sale s = removedTransactions.get(i);
            m.addRow(new Object[]{s.id, s.date.format(fmt), s.cashier, String.format("%.2f", s.total), s.removedAt.format(fmt), s.removedBy});
        }
        JTable t = new JTable(m); styleTable(t);
        JScrollPane sp = new JScrollPane(t); sp.setPreferredSize(new Dimension(760, 280));
        JOptionPane.showMessageDialog(this, sp, "Removed History (newest removal first)", JOptionPane.PLAIN_MESSAGE);
    }

    // ===================== SAVING AND LOADING =====================
    // Everything is kept in plain text files next to the program, one record per line.
    private static final String PRODUCTS_FILE = "products.txt", USERS_FILE = "users.txt", SALES_FILE = "sales.txt";

    /** Load saved data. On the very first run (no files yet) the sample data is created instead. */
    private void loadAll() {
        boolean haveProducts = loadProducts();
        boolean haveUsers = loadUsers();
        if (!haveProducts) seedProducts();
        if (!haveUsers) seedUsers();            // also protects against being locked out with no accounts
        loadSales();                            // needs the products to exist first
        loadRemovedHistory();
    }

    /** Save everything. Called after every change and once more when the window closes. */
    private void saveAll() {
        saveProducts(); saveUsers(); saveSales(); saveRemovedHistory();
    }

    /** Write the lines to a temporary file first, then swap it in, so a crash can never leave a half-written file. */
    private void writeLines(String fileName, java.util.List<String> lines) {
        try {
            java.nio.file.Path target = java.nio.file.Paths.get(fileName), tmp = java.nio.file.Paths.get(fileName + ".tmp");
            java.nio.file.Files.write(tmp, lines, java.nio.charset.StandardCharsets.UTF_8);
            java.nio.file.Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.io.IOException e) { warn("Could not save " + fileName + "."); }
    }

    private java.util.List<String> readLines(String fileName) {
        try { return java.nio.file.Files.readAllLines(java.nio.file.Paths.get(fileName), java.nio.charset.StandardCharsets.UTF_8); }
        catch (java.io.IOException e) { return null; }      // null = no usable file
    }

    private String itemsToText(CartItem[] items) {
        StringBuilder b = new StringBuilder();
        for (CartItem ci : items) {
            if (b.length() > 0) b.append(';');
            b.append(clean(ci.product.id)).append('~').append(clean(ci.product.name)).append('~').append(ci.product.price).append('~').append(ci.quantity);
        }
        return b.toString();
    }

    private CartItem[] textToItems(String text) {
        java.util.ArrayList<CartItem> list = new java.util.ArrayList<>();
        for (String it : text.split(";")) {
            if (it.isEmpty()) continue;
            String[] f = it.split("~");
            Product prod = findProductById(f[0]);                                  // use the real product if it still exists
            if (prod == null) prod = new Product(f[0], f[1], Double.parseDouble(f[2]), 0);  // otherwise keep a copy of its details
            list.add(new CartItem(prod, Integer.parseInt(f[3])));
        }
        return list.toArray(new CartItem[0]);
    }

    private void saveProducts() {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        for (int i = 0; i < productCount; i++) lines.add(clean(products[i].id) + "|" + clean(products[i].name) + "|" + products[i].price + "|" + products[i].stock);
        writeLines(PRODUCTS_FILE, lines);
    }

    private boolean loadProducts() {
        java.util.List<String> lines = readLines(PRODUCTS_FILE);
        if (lines == null) return false;
        for (String line : lines) {
            try { String[] p = line.split("\\|", -1); addProduct(p[0], p[1], Double.parseDouble(p[2]), Integer.parseInt(p[3])); }
            catch (Exception damagedLine) { /* skip a damaged line */ }
        }
        return true;
    }

    // Usernames and passwords are URL-encoded so any character can be saved safely.
    private String enc(String t) { try { return java.net.URLEncoder.encode(t, "UTF-8"); } catch (java.io.UnsupportedEncodingException e) { return t; } }
    private String dec(String t) { try { return java.net.URLDecoder.decode(t, "UTF-8"); } catch (java.io.UnsupportedEncodingException e) { return t; } }

    private void saveUsers() {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        for (int i = 0; i < userCount; i++) lines.add(users[i].getRole() + "|" + enc(users[i].getUsername()) + "|" + enc(users[i].password) + "|" + enc(users[i].getFullName()));
        writeLines(USERS_FILE, lines);
    }

    private boolean loadUsers() {
        java.util.List<String> lines = readLines(USERS_FILE);
        if (lines == null) return false;
        for (String line : lines) {
            try {
                String[] p = line.split("\\|", -1);
                if (userCount >= users.length) break;
                users[userCount++] = p[0].equals("Admin") ? new Admin(dec(p[1]), dec(p[2]), dec(p[3])) : new Employee(dec(p[1]), dec(p[2]), dec(p[3]));
            } catch (Exception damagedLine) { /* skip a damaged line */ }
        }
        return userCount > 0;
    }

    private void saveSales() {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        for (int i = 0; i < saleCount; i++) {        // oldest first, so loading in order rebuilds the same Stack
            Sale s = sales[i];
            lines.add(s.id + "|" + clean(s.cashier) + "|" + s.total + "|" + s.received + "|" + s.date + "|" + itemsToText(s.items));
        }
        writeLines(SALES_FILE, lines);
    }

    private void loadSales() {
        java.util.List<String> lines = readLines(SALES_FILE);
        if (lines == null) return;
        for (String line : lines) {
            try {
                String[] p = line.split("\\|", -1);
                if (saleCount >= sales.length) break;
                Sale s = new Sale(Integer.parseInt(p[0]), p[1], textToItems(p[5]), Double.parseDouble(p[2]), Double.parseDouble(p[3]), LocalDateTime.parse(p[4]));
                sales[saleCount++] = s; recentTransactions.push(s);      // the Stack is rebuilt too: newest sale ends on top
                if (s.id >= nextSaleId) nextSaleId = s.id + 1;
            } catch (Exception damagedLine) { /* skip a damaged line */ }
        }
    }

    /** Save the removed Stack, one sale per line, bottom of the Stack first. */
    private void saveRemovedHistory() {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        for (int i = 0; i < removedTransactions.size(); i++) {
            Sale s = removedTransactions.get(i);
            lines.add(s.id + "|" + clean(s.cashier) + "|" + s.total + "|" + s.received + "|" + s.date + "|" + s.removedAt + "|" + clean(s.removedBy) + "|" + itemsToText(s.items));
        }
        writeLines(HISTORY_FILE, lines);
    }

    /** Load the removed history at start-up. Pushing line by line rebuilds the same Stack order. */
    private void loadRemovedHistory() {
        java.util.List<String> lines = readLines(HISTORY_FILE);
        if (lines == null) return;
        for (String line : lines) {
            try {
                String[] p = line.split("\\|", -1);
                if (p.length < 8) continue;
                Sale s = new Sale(Integer.parseInt(p[0]), p[1], textToItems(p[7]), Double.parseDouble(p[2]), Double.parseDouble(p[3]), LocalDateTime.parse(p[4]));
                s.removedAt = LocalDateTime.parse(p[5]); s.removedBy = p[6];
                removedTransactions.push(s);
                if (s.id >= nextSaleId) nextSaleId = s.id + 1;    // never reuse a receipt number
            } catch (Exception damagedLine) { /* skip a damaged line */ }
        }
    }

    private String clean(String t) { return t.replaceAll("[|;~\\r\\n]", " "); }

    private void showEmployees() {
        if(!(currentUser instanceof Admin)){deny();return;}
        content.removeAll(); JPanel page=new JPanel(new BorderLayout(0,14)); page.setOpaque(false);
        JLabel title=new JLabel("Employee Management"); title.setFont(new Font("SansSerif",Font.BOLD,24)); title.setForeground(TEXT);
        DefaultTableModel model=new DefaultTableModel(new Object[]{"Username","Full Name","Role"},0){public boolean isCellEditable(int r,int c){return false;}};
        for(int i=0;i<userCount;i++)model.addRow(new Object[]{users[i].getUsername(),users[i].getFullName(),users[i] instanceof Admin?"Admin":"Employee"});
        JTable table=new JTable(model);styleTable(table);
        JButton add=button("+ Add Employee",GREEN,Color.WHITE); add.addActionListener(e->{
            JTextField un=new JTextField(), pw=new JTextField(), fn=new JTextField(); Object[] fields={"Username",un,"Temporary password",pw,"Full name",fn};
            if(JOptionPane.showConfirmDialog(this,fields,"Register Employee",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
            if(un.getText().trim().isEmpty()||pw.getText().isEmpty()||fn.getText().trim().isEmpty()){warn("All fields are required.");return;}
            if(findUser(un.getText().trim())!=null){warn("Username already exists.");return;} if(userCount==users.length){warn("Employee list is full.");return;}
            users[userCount++]=new Employee(un.getText().trim(),pw.getText(),fn.getText().trim()); saveAll(); showEmployees();
        });
        JButton remove=button("Remove Selected",new Color(250,232,230),new Color(150,55,45)); remove.addActionListener(e->{int r=table.getSelectedRow();if(r<0){warnSelect();return;}String un=String.valueOf(model.getValueAt(r,0));if(un.equals(currentUser.getUsername())){warn("You cannot remove the account currently in use.");return;}if(JOptionPane.showConfirmDialog(this,"Remove employee account?","Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;for(int i=0;i<userCount;i++)if(users[i].getUsername().equals(un)){for(int j=i;j<userCount-1;j++)users[j]=users[j+1];users[--userCount]=null;break;}saveAll();showEmployees();});
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));actions.setOpaque(false);actions.add(add);actions.add(remove);page.add(title,BorderLayout.NORTH);page.add(new JScrollPane(table),BorderLayout.CENTER);page.add(actions,BorderLayout.SOUTH);content.add(page);content.revalidate();content.repaint();
    }

    private void styleTable(JTable table) { table.setRowHeight(30); table.setFont(new Font("SansSerif",Font.PLAIN,13)); table.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,13)); table.getTableHeader().setBackground(LIGHT_GREEN); table.getTableHeader().setForeground(GREEN); table.setSelectionBackground(new Color(213,231,220)); table.setGridColor(new Color(230,235,232)); }
    private JButton button(String text,Color bg,Color fg){JButton b=new JButton(text);b.setFont(new Font("SansSerif",Font.BOLD,13));b.setBackground(bg);b.setForeground(fg);b.setFocusPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(bg.equals(Color.WHITE)?new Color(220,228,223):bg),new EmptyBorder(9,12,9,12)));return b;}
    private void addProduct(String id,String name,double price,int stock){if(productCount<products.length)products[productCount++]=new Product(id,name,price,stock);}
    private Product findProductById(String id){for(int i=0;i<productCount;i++)if(products[i].id.equals(id))return products[i];return null;}
    private User findUser(String username){for(int i=0;i<userCount;i++)if(users[i].getUsername().equals(username))return users[i];return null;}
    private int lowStockCount(){int n=0;for(int i=0;i<productCount;i++)if(products[i].stock<=5)n++;return n;}
    private double getAllSales(){double n=0;for(int i=0;i<saleCount;i++)n+=sales[i].total;return n;}
    private double getTodaySales(){double n=0;java.time.LocalDate today=java.time.LocalDate.now();for(int i=0;i<saleCount;i++)if(sales[i].date.toLocalDate().equals(today))n+=sales[i].total;return n;}
    private void warn(String s){JOptionPane.showMessageDialog(this,s,"Please check",JOptionPane.WARNING_MESSAGE);}
    private void warnSelect(){warn("Please select a row first.");}
    private void deny(){warn("You do not have permission to access this feature.");}

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> { try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch(Exception ignored) {} new POSSystem().setVisible(true); }); }

    static abstract class User {
        private final String username, password, fullName;
        User(String username,String password,String fullName){this.username=username;this.password=password;this.fullName=fullName;}
        String getUsername(){return username;} String getFullName(){return fullName;}
        boolean login(String entered){return password.equals(entered);}
        abstract String getRole();
    }
    static class Admin extends User { Admin(String u,String p,String f){super(u,p,f);} String getRole(){return "Admin";} }
    static class Employee extends User { Employee(String u,String p,String f){super(u,p,f);} String getRole(){return "Employee";} }
    static class Product { String id,name; double price; int stock; Product(String id,String name,double price,int stock){this.id=id;this.name=name;this.price=price;this.stock=stock;} }
    static class CartItem { Product product; int quantity; CartItem(Product p,int q){product=p;quantity=q;} }
    static class Sale { LocalDateTime removedAt; String removedBy; int id; String cashier; CartItem[] items; double total,received; LocalDateTime date; Sale(int id,String cashier,CartItem[] items,double total,double received,LocalDateTime date){this.id=id;this.cashier=cashier;this.items=items;this.total=total;this.received=received;this.date=date;} }
    static class TransactionStack {
        private Sale[] items; private int top=-1;
        TransactionStack(int capacity){items=new Sale[capacity];}
        boolean isEmpty(){return top==-1;}
        int size(){return top+1;}
        Sale get(int i){return items[i];}   // i=0 is the bottom, size()-1 is the top
        void push(Sale sale){if(top<items.length-1)items[++top]=sale;}
        Sale peek(){return isEmpty()?null:items[top];}
        Sale pop(){if(isEmpty())return null;Sale s=items[top];items[top--]=null;return s;}
    }
}