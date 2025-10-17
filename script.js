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

        // Auth buttons
        document.getElementById('loginBtn').addEventListener('click', () => this.showModal('loginModal'));
        document.getElementById('registerBtn').addEventListener('click', () => this.showModal('registerModal'));
        document.getElementById('logoutBtn').addEventListener('click', () => this.logout());

        // Forms
        document.getElementById('loginForm').addEventListener('submit', (e) => this.handleLogin(e));
        document.getElementById('registerForm').addEventListener('submit', (e) => this.handleRegister(e));
        document.getElementById('reviewForm').addEventListener('submit', (e) => this.handleReviewSubmit(e));

        // Cart and orders
        document.getElementById('checkoutBtn').addEventListener('click', () => this.handleCheckout());
        
        // Search and filter
        document.getElementById('searchMenu').addEventListener('input', (e) => this.filterMenu(e.target.value));
        document.getElementById('categoryFilter').addEventListener('change', (e) => this.filterMenuByCategory(e.target.value));

        // Modal close buttons
        document.querySelectorAll('.close').forEach(closeBtn => {
            closeBtn.addEventListener('click', (e) => {
                e.target.closest('.modal').style.display = 'none';
            });
        });

        // Star rating
        document.querySelectorAll('.stars i').forEach(star => {
            star.addEventListener('click', (e) => this.setRating(e.target));
        });

        // Admin panel
        const adminToggle = document.getElementById('adminToggle');
        const addItemBtn = document.getElementById('addItemBtn');
        if (adminToggle) {
            adminToggle.addEventListener('click', () => this.toggleAdminPanel());
        }
        if (addItemBtn) {
            addItemBtn.addEventListener('click', () => this.showAddItemModal());
        }

        // Close modals when clicking outside
        window.addEventListener('click', (e) => {
            if (e.target.classList.contains('modal')) {
                e.target.style.display = 'none';
            }
        });

        // Mobile menu toggle
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
            // For now, use mock data - replace with actual API call later
            this.menuItems = await this.getMockMenuData();
            this.renderMenu();
            this.populateReviewItems();
        } catch (error) {
            this.showNotification('Failed to load menu', 'error');
        } finally {
            this.showLoading(false);
        }
    }

    // Mock data method - replace with actual API calls
    getMockMenuData() {
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve([
                    { 
                        id: 1, 
                        name: 'Veg Thali', 
                        description: 'Delicious veg thali with rice, dal, vegetables, chapati, and salad', 
                        price: 80, 
                        category: 'veg', 
                        icon: 'leaf',
                        available: true
                    },
                    { 
                        id: 2, 
                        name: 'Chapati & Curry', 
                        description: '2 soft chapatis with mixed vegetable curry', 
                        price: 40, 
                        category: 'veg', 
                        icon: 'bread-slice',
                        available: true
                    },
                    { 
                        id: 3, 
                        name: 'Masala Dosa', 
                        description: 'Crispy dosa with potato masala and chutney', 
                        price: 50, 
                        category: 'veg', 
                        icon: 'pancake',
                        available: true
                    },
                    { 
                        id: 4, 
                        name: 'Chicken Biryani', 
                        description: 'Flavorful basmati rice with tender chicken pieces', 
                        price: 120, 
                        category: 'non-veg', 
                        icon: 'drumstick-bite',
                        available: true
                    },
                    { 
                        id: 5, 
                        name: 'Paneer Butter Masala', 
                        description: 'Cottage cheese in rich buttery tomato gravy', 
                        price: 90, 
                        category: 'veg', 
                        icon: 'cheese',
                        available: true
                    },
                    { 
                        id: 6, 
                        name: 'Chocolate Shake', 
                        description: 'Creamy chocolate milkshake', 
                        price: 60, 
                        category: 'veg', 
                        icon: 'glass-whiskey',
                        available: true
                    }
                ]);
            }, 500);
        });
    }

    renderMenu() {
        const container = document.getElementById('menuItems');
        if (!container) return;

        container.innerHTML = this.menuItems.map(item => `
            <div class="menu-item" data-category="${item.category || 'veg'}">
                <div class="menu-item-image">
                    <i class="fas fa-${item.icon || 'utensils'}"></i>
                </div>
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

        const quantity = this.cart.get(itemId) || 0;
        if (quantity > 0) {
            this.cart.set(itemId, quantity);
            this.showNotification('Item updated in cart', 'success');
        } else {
            this.cart.set(itemId, 1);
            this.showNotification('Item added to cart', 'success');
        }

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
        if (current > 1) {
            this.cart.set(itemId, current - 1);
        } else {
            this.cart.delete(itemId);
        }
        this.updateQuantityDisplay(itemId);
    }

    updateQuantityDisplay(itemId) {
        const element = document.getElementById(`quantity-${itemId}`);
        if (element) {
            element.textContent = this.cart.get(itemId) || 0;
        }
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
            if (item) {
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
        if (!timeSlotSelect) {
            this.showNotification('Time slot selection not found', 'error');
            return;
        }

        const timeSlot = timeSlotSelect.value;
        if (!timeSlot) {
            this.showNotification('Please select a time slot', 'warning');
            return;
        }

        this.showLoading(true);
        try {
            // Convert cart to the format needed for order
            const itemsMap = {};
            for (const [itemId, quantity] of this.cart) {
                itemsMap[itemId] = quantity;
            }

            const orderData = {
                userId: this.currentUser.id,
                items: itemsMap,
                timeSlot: timeSlot
            };

            // For now, simulate API call - replace with actual API later
            const order = await this.simulateOrder(orderData);
            
            // Process payment
            const paymentResult = await this.simulatePayment({
                orderId: order.id,
                amount: this.calculateTotal(),
                method: 'UPI'
            });

            if (paymentResult.success) {
                this.showNotification('Order placed successfully!', 'success');
                this.cart.clear();
                this.updateCartDisplay();
                this.updateCartCount();
                this.showSection('orders');
                await this.loadUserOrders();
            }
        } catch (error) {
            this.showNotification('Failed to place order: ' + error.message, 'error');
        } finally {
            this.showLoading(false);
        }
    }

    simulateOrder(orderData) {
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({
                    id: Math.floor(Math.random() * 1000) + 1,
                    status: 'PLACED',
                    totalAmount: this.calculateTotal(),
                    items: orderData.items,
                    timeSlot: orderData.timeSlot
                });
            }, 1000);
        });
    }

    simulatePayment(paymentData) {
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({
                    success: true,
                    transactionId: 'TXN' + Date.now(),
                    amount: paymentData.amount
                });
            }, 500);
        });
    }

    calculateTotal() {
        let subtotal = 0;
        for (const [itemId, quantity] of this.cart) {
            const item = this.menuItems.find(m => m.id === itemId);
            if (item) {
                subtotal += item.price * quantity;
            }
        }
        return subtotal + (subtotal * 0.05); // Including 5% tax
    }

    async handleLogin(e) {
        e.preventDefault();
        const emailInput = document.getElementById('loginEmail');
        const passwordInput = document.getElementById('loginPassword');
        
        if (!emailInput || !passwordInput) {
            this.showNotification('Login form not found', 'error');
            return;
        }

        const email = emailInput.value;
        const password = passwordInput.value;

        if (!email || !password) {
            this.showNotification('Please enter email and password', 'warning');
            return;
        }

        this.showLoading(true);
        try {
            // Simulate API call - replace with actual API later
            const user = await this.simulateLogin({ email, password });
            this.currentUser = user;
            localStorage.setItem('currentUser', JSON.stringify(this.currentUser));
            this.updateAuthUI();
            this.showModal('loginModal', false);
            this.showNotification('Login successful!', 'success');
            await this.loadUserOrders();
            await this.loadReviews();
        } catch (error) {
            this.showNotification('Login failed: ' + error.message, 'error');
        } finally {
            this.showLoading(false);
        }
    }

    simulateLogin(credentials) {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                if (credentials.email === 'demo@example.com' && credentials.password === 'password') {
                    resolve({
                        id: 1,
                        name: 'Demo User',
                        email: credentials.email,
                        wallet: 1000.00,
                        role: 'user'
                    });
                } else {
                    reject(new Error('Invalid credentials'));
                }
            }, 1000);
        });
    }

    async handleRegister(e) {
        e.preventDefault();
        const nameInput = document.getElementById('registerName');
        const emailInput = document.getElementById('registerEmail');
        const passwordInput = document.getElementById('registerPassword');
        
        if (!nameInput || !emailInput || !passwordInput) {
            this.showNotification('Registration form not found', 'error');
            return;
        }

        const name = nameInput.value;
        const email = emailInput.value;
        const password = passwordInput.value;

        if (!name || !email || !password) {
            this.showNotification('Please fill all fields', 'warning');
            return;
        }

        this.showLoading(true);
        try {
            // Simulate API call - replace with actual API later
            await this.simulateRegister({ name, email, password });
            this.showModal('registerModal', false);
            this.showNotification('Registration successful! Please login.', 'success');
            
            // Clear form
            nameInput.value = '';
            emailInput.value = '';
            passwordInput.value = '';
        } catch (error) {
            this.showNotification('Registration failed: ' + error.message, 'error');
        } finally {
            this.showLoading(false);
        }
    }

    simulateRegister(userData) {
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({ success: true });
            }, 1000);
        });
    }

    async handleReviewSubmit(e) {
        e.preventDefault();
        if (!this.currentUser) {
            this.showNotification('Please login to submit review', 'warning');
            return;
        }

        const menuItemSelect = document.getElementById('reviewItem');
        const ratingInput = document.getElementById('rating');
        const commentInput = document.getElementById('reviewComment');
        
        if (!menuItemSelect || !ratingInput || !commentInput) {
            this.showNotification('Review form not found', 'error');
            return;
        }

        const menuItemId = menuItemSelect.value;
        const rating = ratingInput.value;
        const comment = commentInput.value;

        if (!menuItemId || !rating) {
            this.showNotification('Please select item and rating', 'warning');
            return;
        }

        this.showLoading(true);
        try {
            // Simulate API call - replace with actual API later
            await this.simulateReview({
                userId: this.currentUser.id,
                menuItemId: parseInt(menuItemId),
                rating: parseInt(rating),
                comment: comment
            });

            this.showNotification('Review submitted successfully!', 'success');
            document.getElementById('reviewForm').reset();
            this.resetStars();
            await this.loadReviews();
        } catch (error) {
            this.showNotification('Failed to submit review: ' + error.message, 'error');
        } finally {
            this.showLoading(false);
        }
    }

    simulateReview(reviewData) {
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({ success: true });
            }, 1000);
        });
    }

    setRating(starElement) {
        const rating = starElement.getAttribute('data-rating');
        const stars = starElement.parentElement.querySelectorAll('i');
        
        stars.forEach((star, index) => {
            if (index < rating) {
                star.classList.add('active');
                star.classList.remove('far');
                star.classList.add('fas');
            } else {
                star.classList.remove('active');
                star.classList.remove('fas');
                star.classList.add('far');
            }
        });

        const ratingInput = document.getElementById('rating');
        if (ratingInput) {
            ratingInput.value = rating;
        }
    }

    resetStars() {
        const stars = document.querySelectorAll('.stars i');
        stars.forEach(star => {
            star.classList.remove('active');
            star.classList.remove('fas');
            star.classList.add('far');
        });
        const ratingInput = document.getElementById('rating');
        if (ratingInput) {
            ratingInput.value = '';
        }
    }

    populateReviewItems() {
        const select = document.getElementById('reviewItem');
        if (select) {
            select.innerHTML = '<option value="">Select Menu Item</option>' +
                this.menuItems.map(item => 
                    `<option value="${item.id}">${item.name}</option>`
                ).join('');
        }
    }

    async loadUserOrders() {
        if (!this.currentUser) return;

        try {
            // Simulate loading orders - replace with actual API later
            this.orders = await this.simulateGetOrders(this.currentUser.id);
            this.renderOrders();
        } catch (error) {
            console.error('Failed to load orders:', error);
        }
    }

    simulateGetOrders(userId) {
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve([
                    { 
                        id: 1, 
                        status: 'COMPLETED', 
                        totalAmount: 160, 
                        timeSlot: '12:30-12:45', 
                        createdAt: new Date().toISOString(), 
                        items: [
                            { name: 'Veg Thali', quantity: 1, price: 80 },
                            { name: 'Chapati & Curry', quantity: 2, price: 40 }
                        ]
                    }
                ]);
            }, 500);
        });
    }

    renderOrders() {
        const container = document.getElementById('ordersList');
        if (!container) return;

        container.innerHTML = this.orders.map(order => `
            <div class="order-card">
                <div class="order-header">
                    <div>
                        <h3>Order #${order.id}</h3>
                        <small>Placed on ${new Date(order.createdAt).toLocaleDateString()}</small>
                    </div>
                    <div class="order-status status-${order.status.toLowerCase()}">
                        ${order.status}
                    </div>
                </div>
                <div class="order-items">
                    ${order.items.map(item => `
                        <div class="order-item">
                            <span>${item.name} x${item.quantity}</span>
                            <span>₹${(item.price * item.quantity).toFixed(2)}</span>
                        </div>
                    `).join('')}
                </div>
                <div class="order-total">
                    Total: ₹${order.totalAmount.toFixed(2)}
                </div>
                <div class="time-slot">
                    Time Slot: ${order.timeSlot}
                </div>
            </div>
        `).join('') || '<p class="text-center">No orders found</p>';
    }

    async loadReviews() {
        try {
            // Simulate loading reviews - replace with actual API later
            this.reviews = await this.simulateGetReviews();
            this.renderReviews();
        } catch (error) {
            console.error('Failed to load reviews:', error);
        }
    }

    simulateGetReviews() {
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve([
                    { 
                        id: 1, 
                        userName: 'Demo User', 
                        menuItemId: 1, 
                        rating: 5, 
                        comment: 'Absolutely delicious! The thali was fresh and flavorful.', 
                        createdAt: new Date().toISOString() 
                    },
                    { 
                        id: 2, 
                        userName: 'Test User', 
                        menuItemId: 3, 
                        rating: 4, 
                        comment: 'Masala Dosa was crispy and tasty. Good portion size.', 
                        createdAt: new Date(Date.now() - 86400000).toISOString() 
                    }
                ]);
            }, 500);
        });
    }

    renderReviews() {
        const container = document.getElementById('reviewsList');
        if (!container) return;

        container.innerHTML = this.reviews.map(review => {
            const menuItem = this.menuItems.find(m => m.id === review.menuItemId);
            return `
                <div class="review-card">
                    <div class="review-header">
                        <div class="review-author">${review.userName}</div>
                        <div class="review-rating">
                            ${'★'.repeat(review.rating)}${'☆'.repeat(5 - review.rating)}
                        </div>
                    </div>
                    <div class="review-item">
                        ${menuItem ? menuItem.name : 'Menu Item'}
                    </div>
                    <div class="review-comment">${review.comment}</div>
                    <div class="review-date">
                        ${new Date(review.createdAt).toLocaleDateString()}
                    </div>
                </div>
            `;
        }).join('') || '<p class="text-center">No reviews yet</p>';
    }

    filterMenu(searchTerm) {
        const items = document.querySelectorAll('.menu-item');
        items.forEach(item => {
            const name = item.querySelector('.menu-item-name').textContent.toLowerCase();
            const description = item.querySelector('.menu-item-description').textContent.toLowerCase();
            const matchesSearch = name.includes(searchTerm.toLowerCase()) || 
                                description.includes(searchTerm.toLowerCase());
            item.style.display = matchesSearch ? 'block' : 'none';
        });
    }

    filterMenuByCategory(category) {
        const items = document.querySelectorAll('.menu-item');
        items.forEach(item => {
            const itemCategory = item.getAttribute('data-category');
            const matchesCategory = category === 'all' || itemCategory === category;
            item.style.display = matchesCategory ? 'block' : 'none';
        });
    }

    showSection(sectionName) {
        // Hide all sections
        document.querySelectorAll('.section').forEach(section => {
            section.classList.remove('active');
        });

        // Remove active class from all nav links
        document.querySelectorAll('.nav-link').forEach(link => {
            link.classList.remove('active');
        });

        // Show selected section and activate nav link
        const sectionElement = document.getElementById(sectionName);
        const navLink = document.querySelector(`[data-section="${sectionName}"]`);
        
        if (sectionElement) sectionElement.classList.add('active');
        if (navLink) navLink.classList.add('active');

        // Load section-specific data
        if (sectionName === 'cart') {
            this.updateCartDisplay();
        } else if (sectionName === 'orders') {
            this.loadUserOrders();
        } else if (sectionName === 'reviews') {
            this.loadReviews();
        }

        // Close mobile menu
        const navLinks = document.querySelector('.nav-links');
        if (navLinks) {
            navLinks.classList.remove('active');
        }
    }

    showModal(modalId, show = true) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.style.display = show ? 'block' : 'none';
        }
    }

    showLoading(show) {
        const loading = document.getElementById('loading');
        if (loading) {
            loading.classList.toggle('hidden', !show);
        }
    }

    showNotification(message, type = 'info') {
        const notification = document.getElementById('notification');
        if (notification) {
            notification.textContent = message;
            notification.className = `notification ${type} show`;
            
            setTimeout(() => {
                notification.classList.remove('show');
            }, 3000);
        }
    }

    updateAuthUI() {
        const loginBtn = document.getElementById('loginBtn');
        const registerBtn = document.getElementById('registerBtn');
        const userMenu = document.getElementById('userMenu');
        const userName = document.getElementById('userName');
        const adminPanel = document.getElementById('adminPanel');

        if (this.currentUser) {
            if (loginBtn) loginBtn.classList.add('hidden');
            if (registerBtn) registerBtn.classList.add('hidden');
            if (userMenu) userMenu.classList.remove('hidden');
            if (userName) userName.textContent = this.currentUser.name;
            
            // Show admin panel for admin users
            if (adminPanel && this.currentUser.role === 'admin') {
                adminPanel.classList.remove('hidden');
            }
        } else {
            if (loginBtn) loginBtn.classList.remove('hidden');
            if (registerBtn) registerBtn.classList.remove('hidden');
            if (userMenu) userMenu.classList.add('hidden');
            if (adminPanel) adminPanel.classList.add('hidden');
        }
    }

    logout() {
        this.currentUser = null;
        this.cart.clear();
        localStorage.removeItem('currentUser');
        this.updateAuthUI();
        this.updateCartCount();
        this.showNotification('Logged out successfully', 'success');
        this.showSection('home');
    }

    checkAuthStatus() {
        // Check if user is logged in (from localStorage)
        try {
            const savedUser = localStorage.getItem('currentUser');
            if (savedUser) {
                this.currentUser = JSON.parse(savedUser);
                this.updateAuthUI();
                this.loadUserOrders();
                this.loadReviews();
            }
        } catch (error) {
            console.error('Error loading saved user:', error);
            localStorage.removeItem('currentUser');
        }
    }

    toggleAdminPanel() {
        const content = document.querySelector('.admin-content');
        if (content) {
            content.style.display = content.style.display === 'block' ? 'none' : 'block';
        }
    }

    showAddItemModal() {
        this.showNotification('Admin feature: Add menu item - Coming soon!', 'info');
    }
}

// Initialize the application when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    window.app = new CanteenApp();
});