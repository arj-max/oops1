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
        // Navigation
        document.querySelectorAll('.nav-link').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const section = e.target.getAttribute('data-section');
                this.showSection(section);
            });
        });

        // Login/Register buttons
        document.getElementById('loginBtn').addEventListener('click', () => this.showModal('loginModal'));
        document.getElementById('registerBtn').addEventListener('click', () => this.showModal('registerModal'));
        document.getElementById('logoutBtn').addEventListener('click', () => this.logout());

        // Forms
        document.getElementById('loginForm').addEventListener('submit', (e) => this.handleLogin(e));
        document.getElementById('registerForm').addEventListener('submit', (e) => this.handleRegister(e));
        document.getElementById('reviewForm').addEventListener('submit', (e) => this.handleReviewSubmit(e));

        // Checkout
        document.getElementById('checkoutBtn').addEventListener('click', () => this.handleCheckout());

        // Search & filter
        document.getElementById('searchMenu').addEventListener('input', (e) => this.filterMenu(e.target.value));
        document.getElementById('categoryFilter').addEventListener('change', (e) => this.filterMenuByCategory(e.target.value));

        // Close modals
        document.querySelectorAll('.close').forEach(btn => {
            btn.addEventListener('click', (e) => e.target.closest('.modal').style.display = 'none');
        });

        // Stars for reviews
        document.querySelectorAll('.stars i').forEach(star => {
            star.addEventListener('click', (e) => this.setRating(e.target));
        });

        // Admin panel
        const adminToggle = document.getElementById('adminToggle');
        const addItemBtn = document.getElementById('addItemBtn');
        if (adminToggle) adminToggle.addEventListener('click', () => this.toggleAdminPanel());
        if (addItemBtn) addItemBtn.addEventListener('click', () => this.showAddItemModal());

        // Close modal by clicking outside
        window.addEventListener('click', (e) => {
            if (e.target.classList.contains('modal')) e.target.style.display = 'none';
        });

        // Mobile nav toggle
        const navToggle = document.querySelector('.nav-toggle');
        if (navToggle) {
            navToggle.addEventListener('click', () => {
                document.querySelector('.nav-links').classList.toggle('active');
            });
        }
    }

    showModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) modal.style.display = 'block';
    }

    showSection(sectionId) {
        document.querySelectorAll('.section').forEach(sec => sec.classList.remove('active'));
        const section = document.getElementById(sectionId);
        if (section) section.classList.add('active');

        // Highlight active nav link
        document.querySelectorAll('.nav-link').forEach(link => link.classList.remove('active'));
        const activeLink = document.querySelector(`.nav-link[data-section="${sectionId}"]`);
        if (activeLink) activeLink.classList.add('active');
    }

    showNotification(message, type = 'success') {
        const notification = document.getElementById('notification');
        if (!notification) return;
        notification.textContent = message;
        notification.className = `notification show ${type}`;
        setTimeout(() => notification.className = 'notification', 3000);
    }

    showLoading(show) {
        const loading = document.getElementById('loading');
        if (!loading) return;
        loading.classList.toggle('hidden', !show);
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
        this.updateCartDisplay();
        this.updateCartCount();
    }

    decreaseQuantity(itemId) {
        const current = this.cart.get(itemId) || 0;
        if (current > 1) this.cart.set(itemId, current - 1);
        else this.cart.delete(itemId);
        this.updateQuantityDisplay(itemId);
        this.updateCartDisplay();
        this.updateCartCount();
    }

    updateQuantityDisplay(itemId) {
        const el = document.getElementById(`quantity-${itemId}`);
        if (el) el.textContent = this.cart.get(itemId) || 0;
    }

    updateCartDisplay() {
        const container = document.getElementById('cartItems');
        const subtotalEl = document.getElementById('subtotal');
        const taxEl = document.getElementById('tax');
        const totalEl = document.getElementById('total');
        if (!container || !subtotalEl) return;

        let subtotal = 0;
        let html = '';

        for (const [itemId, qty] of this.cart) {
            const item = this.menuItems.find(m => m.id === itemId);
            if (!item) continue;
            const itemTotal = item.price * qty;
            subtotal += itemTotal;

            html += `
                <div class="cart-item">
                    <div class="cart-item-info">
                        <div class="cart-item-details">
                            <h4>${item.name}</h4>
                            <div class="cart-item-price">₹${item.price} each</div>
                        </div>
                    </div>
                    <div class="quantity-controls">
                        <button class="quantity-btn" onclick="app.decreaseQuantity(${item.id})">-</button>
                        <span>${qty}</span>
                        <button class="quantity-btn" onclick="app.increaseQuantity(${item.id})">+</button>
                        <button class="btn btn-outline" onclick="app.removeFromCart(${item.id})">Remove</button>
                    </div>
                    <div class="cart-item-total">₹${itemTotal.toFixed(2)}</div>
                </div>
            `;
        }

        const tax = subtotal * 0.05;
        const total = subtotal + tax;

        container.innerHTML = html || '<p class="text-center">Your cart is empty</p>';
        subtotalEl.textContent = `₹${subtotal.toFixed(2)}`;
        if (taxEl) taxEl.textContent = `₹${tax.toFixed(2)}`;
        if (totalEl) totalEl.textContent = `₹${total.toFixed(2)}`;
    }

    removeFromCart(itemId) {
        this.cart.delete(itemId);
        this.updateCartDisplay();
        this.updateCartCount();
        this.updateQuantityDisplay(itemId);
        this.showNotification('Item removed from cart', 'success');
    }

    updateCartCount() {
        const countEl = document.getElementById('cartCount');
        if (!countEl) return;
        const count = Array.from(this.cart.values()).reduce((sum, qty) => sum + qty, 0);
        countEl.textContent = count;
    }

    // Placeholder: implement login/register/review/payment/checkout logic
    checkAuthStatus() {}
    handleLogin(e) { e.preventDefault(); }
    handleRegister(e) { e.preventDefault(); }
    handleReviewSubmit(e) { e.preventDefault(); }
    filterMenu(searchTerm) {}
    filterMenuByCategory(category) {}
    setRating(starEl) {}
    toggleAdminPanel() {}
    showAddItemModal() {}
    handleCheckout() {}
}

// Initialize app globally
const app = new CanteenApp();
