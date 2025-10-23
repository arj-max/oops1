class CanteenApp {
    constructor() {
        this.currentUser = null;
        this.cart = new Map();
        this.menuItems = [];
        this.orders = [];
        this.reviews = [];
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.loadMenu();
        this.checkAuthStatus();
        this.showSection('home');
    }

    setupEventListeners() {
        document.querySelectorAll('.nav-link').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const section = e.target.getAttribute('data-section');
                this.showSection(section);
            });
        });

        document.getElementById('loginBtn').addEventListener('click', () => this.showModal('loginModal'));
        document.getElementById('registerBtn').addEventListener('click', () => this.showModal('registerModal'));
        document.getElementById('logoutBtn').addEventListener('click', () => this.logout());

        document.getElementById('loginForm').addEventListener('submit', (e) => this.handleLogin(e));
        document.getElementById('registerForm').addEventListener('submit', (e) => this.handleRegister(e));
        document.getElementById('reviewForm').addEventListener('submit', (e) => this.handleReviewSubmit(e));

        document.getElementById('checkoutBtn').addEventListener('click', () => this.handleCheckout());
        document.getElementById('searchMenu').addEventListener('input', (e) => this.filterMenu(e.target.value));
        document.getElementById('categoryFilter').addEventListener('change', (e) => this.filterMenuByCategory(e.target.value));

        document.querySelectorAll('.close').forEach(closeBtn => {
            closeBtn.addEventListener('click', (e) => {
                e.target.closest('.modal').style.display = 'none';
            });
        });

        document.querySelectorAll('.stars i').forEach(star => {
            star.addEventListener('click', (e) => this.setRating(e.target));
        });

        const adminToggle = document.getElementById('adminToggle');
        const addItemBtn = document.getElementById('addItemBtn');
        if (adminToggle) adminToggle.addEventListener('click', () => this.toggleAdminPanel());
        if (addItemBtn) addItemBtn.addEventListener('click', () => this.showAddItemModal());

        window.addEventListener('click', (e) => {
            if (e.target.classList.contains('modal')) e.target.style.display = 'none';
        });

        const navToggle = document.querySelector('.nav-toggle');
        if (navToggle) {
            navToggle.addEventListener('click', () => {
                document.querySelector('.nav-links').classList.toggle('active');
            });
        }
    }

    async loadMenu() {
        this.showLoading(true);
        try {
            const response = await fetch('/api/menu');
            if (!response.ok) throw new Error('Failed to fetch menu');
            this.menuItems = await response.json();
            this.renderMenu();
            this.populateReviewItems();
        } catch (error) {
            this.showNotification('Failed to load menu', 'error');
        } finally {
            this.showLoading(false);
        }
    }

    renderMenu() {
        const container = document.getElementById('menuItems');
        if (!container) return;

        container.innerHTML = this.menuItems.map(item => `
            <div class="menu-item" data-category="${item.category || 'veg'}">
                <div class="menu-item-image"><i class="fas fa-${item.icon || 'utensils'}"></i></div>
                <div class="menu-item-content">
                    <div class="menu-item-header">
                        <div class="menu-item-name">${item.name}</div>
                        <div class="menu-item-price">₹${item.price}</div>
                    </div>
                    <div class="menu-item-description">${item.description}</div>
                    <div class="menu-item-actions">
                        <div class="quantity-controls">
                            <button class="quantity-btn" onclick="app.decreaseQuantity(${item.id})">-</button>
                            <span id="quantity-${item.id}">${this.cart.get(item.id) || 0}</span>
                            <button class="quantity-btn" onclick="app.increaseQuantity(${item.id})">+</button>
                        </div>
                        <button class="btn btn-primary" onclick="app.addToCart(${item.id})">
                            ${this.cart.has(item.id) ? 'Update' : 'Add to Cart'}
                        </button>
                    </div>
                </div>
            </div>
        `).join('');
    }

    addToCart(itemId) {
        if (!this.currentUser) {
            this.showNotification('Please login to add items to cart', 'warning');
            this.showModal('loginModal');
            return;
        }
        const quantity = this.cart.get(itemId) || 1;
        this.cart.set(itemId, quantity);
        this.showNotification('Item added/updated in cart', 'success');
        this.updateCartDisplay();
        this.updateCartCount();
    }

    increaseQuantity(itemId) {
        const current = this.cart.get(itemId) || 0;
        this.cart.set(itemId, current + 1);
        this.updateQuantityDisplay(itemId);
    }

    decreaseQuantity(itemId) {
        const current = this.cart.get(itemId) || 0;
        if (current > 1) this.cart.set(itemId, current - 1);
        else this.cart.delete(itemId);
        this.updateQuantityDisplay(itemId);
    }

    updateQuantityDisplay(itemId) {
        const element = document.getElementById(`quantity-${itemId}`);
        if (element) element.textContent = this.cart.get(itemId) || 0;
    }

    updateCartDisplay() {
        const container = document.getElementById('cartItems');
        const subtotalElement = document.getElementById('subtotal');
        const taxElement = document.getElementById('tax');
        const totalElement = document.getElementById('total');
        if (!container || !subtotalElement) return;

        let subtotal = 0;
        let cartHTML = '';

        for (const [itemId, quantity] of this.cart) {
            const item = this.menuItems.find(m => m.id === itemId);
            if (!item) continue;
            const itemTotal = item.price * quantity;
            subtotal += itemTotal;

            cartHTML += `
                <div class="cart-item">
                    <div class="cart-item-info">
                        <div class="cart-item-details">
                            <h4>${item.name}</h4>
                            <div class="cart-item-price">₹${item.price} each</div>
                        </div>
                    </div>
                    <div class="quantity-controls">
                        <button class="quantity-btn" onclick="app.decreaseQuantity(${item.id})">-</button>
                        <span>${quantity}</span>
                        <button class="quantity-btn" onclick="app.increaseQuantity(${item.id})">+</button>
                        <button class="btn btn-outline" onclick="app.removeFromCart(${item.id})">Remove</button>
                    </div>
                    <div class="cart-item-total">₹${itemTotal.toFixed(2)}</div>
                </div>
            `;
        }

        const tax = subtotal * 0.05;
        const total = subtotal + tax;

        container.innerHTML = cartHTML || '<p class="text-center">Your cart is empty</p>';
        subtotalElement.textContent = `₹${subtotal.toFixed(2)}`;
        if (taxElement) taxElement.textContent = `₹${tax.toFixed(2)}`;
        if (totalElement) totalElement.textContent = `₹${total.toFixed(2)}`;
    }

    removeFromCart(itemId) {
        this.cart.delete(itemId);
        this.updateCartDisplay();
        this.updateCartCount();
        this.updateQuantityDisplay(itemId);
        this.showNotification('Item removed from cart', 'success');
    }

    updateCartCount() {
        const countElement = document.getElementById('cartCount');
        if (countElement) {
            const count = Array.from(this.cart.values()).reduce((sum, qty) => sum + qty, 0);
            countElement.textContent = count;
        }
    }

    async handleCheckout() {
        if (!this.currentUser) {
            this.showNotification('Please login to place order', 'warning');
            this.showModal('loginModal');
            return;
        }

        if (this.cart.size === 0) {
            this.showNotification('Your cart is empty', 'warning');
            return;
        }

        const timeSlotSelect = document.getElementById('timeSlot');
        if (!timeSlotSelect || !timeSlotSelect.value) {
            this.showNotification('Please select a time slot', 'warning');
            return;
        }

        this.showLoading(true);
        try {
            const itemsMap = {};
            for (const [itemId, quantity] of this.cart) itemsMap[itemId] = quantity;

            const orderResponse = await fetch('/api/order', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    userId: this.currentUser.id,
                    items: itemsMap,
                    timeSlot: timeSlotSelect.value
                })
            });

            if (!orderResponse.ok) throw new Error('Order failed');
            const order = await orderResponse.json();

            // Payment
            const paymentResponse = await fetch('/api/payment', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ orderId: order.id, amount: this.calculateTotal(), method: 'UPI' })
            });

            const paymentResult = await
